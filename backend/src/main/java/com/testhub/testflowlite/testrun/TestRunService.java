package com.testhub.testflowlite.testrun;

import com.testhub.testflowlite.audit.AuditLogService;
import com.testhub.testflowlite.common.ConflictException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.milestone.Milestone;
import com.testhub.testflowlite.milestone.MilestoneRepository;
import com.testhub.testflowlite.project.Project;
import com.testhub.testflowlite.project.ProjectMemberRepository;
import com.testhub.testflowlite.project.ProjectRepository;
import com.testhub.testflowlite.testcase.TestCase;
import com.testhub.testflowlite.testcase.TestCaseRepository;
import com.testhub.testflowlite.testcase.TestCaseStatus;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestRunService {

    private final TestRunRepository testRunRepository;
    private final TestRunCaseRepository testRunCaseRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MilestoneRepository milestoneRepository;
    private final TestCaseRepository testCaseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<TestRunDto> getTestRuns(Long projectId, String currentUsername) {
        verifyProjectAccess(projectId, currentUsername);
        List<TestRun> runs = testRunRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        return runs.stream().map(r -> mapToDto(r, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TestRunDto getTestRunDetail(Long runId, String currentUsername) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Test Run not found: " + runId));
        verifyProjectAccess(run.getProject().getId(), currentUsername);
        return mapToDto(run, true);
    }

    @Transactional
    public TestRunDto createTestRun(Long projectId, CreateTestRunRequest request, String currentUsername) {
        User currentUser = verifyLeaderAccess(projectId, currentUsername);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        Milestone milestone = null;
        if (request.getMilestoneId() != null) {
            milestone = milestoneRepository.findById(request.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone not found: " + request.getMilestoneId()));
            if (!milestone.getProject().getId().equals(projectId)) {
                throw new IllegalArgumentException("Milestone does not belong to project " + projectId);
            }
        }

        TestRun run = new TestRun();
        run.setProject(project);
        run.setMilestone(milestone);
        run.setName(request.getName().trim());
        run.setStatus(RunStatus.OPEN);
        run.setCreatedBy(currentUser);

        TestRun savedRun = testRunRepository.save(run);

        if (request.getCases() != null && !request.getCases().isEmpty()) {
            addCasesInternal(savedRun, request.getCases(), Boolean.TRUE.equals(request.getIncludeNonReady()));
        }

        auditLogService.logAction(currentUser.getId(), "CREATE_TESTRUN", "TEST_RUN", savedRun.getId(), "Created Test Run " + savedRun.getName());
        return mapToDto(savedRun, true);
    }

    @Transactional
    public TestRunDto addCasesToRun(Long runId, AddCasesToRunRequest request, String currentUsername) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Test Run not found: " + runId));
        verifyLeaderAccess(run.getProject().getId(), currentUsername);

        if (run.getStatus() == RunStatus.CLOSED) {
            throw new ConflictException("Cannot add test cases to a closed Test Run");
        }

        addCasesInternal(run, request.getCases(), Boolean.TRUE.equals(request.getIncludeNonReady()));
        auditLogService.logAction(run.getCreatedBy() != null ? run.getCreatedBy().getId() : null, "ADD_CASES_TO_RUN", "TEST_RUN", run.getId(), "Added cases to Test Run");
        return mapToDto(run, true);
    }

    @Transactional
    public void removeCaseFromRun(Long runId, Long runCaseId, String currentUsername) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Test Run not found: " + runId));
        verifyLeaderAccess(run.getProject().getId(), currentUsername);

        if (run.getStatus() == RunStatus.CLOSED) {
            throw new ConflictException("Cannot remove test case from a closed Test Run");
        }

        TestRunCase runCase = testRunCaseRepository.findById(runCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test Run Case not found: " + runCaseId));

        if (!runCase.getRun().getId().equals(runId)) {
            throw new IllegalArgumentException("Test Run Case " + runCaseId + " does not belong to Test Run " + runId);
        }

        testRunCaseRepository.delete(runCase);
    }

    @Transactional
    public TestRunDto closeTestRun(Long runId, String currentUsername) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Test Run not found: " + runId));
        User currentUser = verifyLeaderAccess(run.getProject().getId(), currentUsername);

        if (run.getStatus() == RunStatus.CLOSED) {
            throw new ConflictException("Test Run is already closed");
        }

        run.setStatus(RunStatus.CLOSED);
        run.setClosedAt(LocalDateTime.now());
        TestRun updated = testRunRepository.save(run);

        auditLogService.logAction(currentUser.getId(), "CLOSE_TESTRUN", "TEST_RUN", updated.getId(), "Closed Test Run " + updated.getName());
        return mapToDto(updated, true);
    }

    private void addCasesInternal(TestRun run, List<RunCaseItem> items, boolean includeNonReady) {
        Long projectId = run.getProject().getId();
        List<TestRunCase> casesToSave = new ArrayList<>();

        for (RunCaseItem item : items) {
            if (testRunCaseRepository.existsByRunIdAndCaseId(run.getId(), item.getCaseId())) {
                continue; // Skip if already added
            }

            TestCase tc = testCaseRepository.findById(item.getCaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Test Case not found: " + item.getCaseId()));

            if (!tc.getSection().getProject().getId().equals(projectId)) {
                throw new IllegalArgumentException("Test Case " + item.getCaseId() + " does not belong to project " + projectId);
            }

            if (!includeNonReady && tc.getStatus() != TestCaseStatus.READY) {
                throw new IllegalArgumentException("Test Case " + tc.getCode() + " is not in READY status");
            }

            User assignedUser = null;
            if (item.getAssignedToId() != null) {
                assignedUser = userRepository.findById(item.getAssignedToId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + item.getAssignedToId()));

                if (!assignedUser.getIsActive()) {
                    throw new IllegalArgumentException("User " + assignedUser.getUsername() + " is disabled");
                }

                if (assignedUser.getRole() != Role.LEADER && !projectMemberRepository.existsByProjectIdAndUserId(projectId, assignedUser.getId())) {
                    throw new IllegalArgumentException("User " + assignedUser.getUsername() + " is not assigned to project " + projectId);
                }
            }

            TestRunCase trc = new TestRunCase();
            trc.setRun(run);
            trc.setCaseId(tc.getId());

            // SNAPSHOT RULE 11: Point in time copy of case fields
            trc.setTitle(tc.getTitle());
            trc.setPrecondition(tc.getPrecondition());
            trc.setSteps(tc.getSteps());
            trc.setExpectedResult(tc.getExpectedResult());
            trc.setTestData(tc.getTestData());

            trc.setAssignedTo(assignedUser);
            trc.setResultStatus(ResultStatus.UNTESTED);
            trc.setIsReviewed(false);

            casesToSave.add(trc);
        }

        if (!casesToSave.isEmpty()) {
            testRunCaseRepository.saveAll(casesToSave);
        }
    }

    private User verifyProjectAccess(Long projectId, String currentUsername) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }

        User user = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        if (user.getRole() != Role.LEADER && !projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new AccessDeniedException("You do not have access to this project");
        }

        return user;
    }

    private User verifyLeaderAccess(Long projectId, String currentUsername) {
        User user = verifyProjectAccess(projectId, currentUsername);
        if (user.getRole() != Role.LEADER) {
            throw new AccessDeniedException("Leader role required for Test Run management");
        }
        return user;
    }

    private TestRunDto mapToDto(TestRun run, boolean includeCases) {
        List<TestRunCase> runCases = testRunCaseRepository.findByRunIdOrderByIdAsc(run.getId());

        int total = runCases.size();
        int passed = (int) runCases.stream().filter(c -> c.getResultStatus() == ResultStatus.PASSED).count();
        int failed = (int) runCases.stream().filter(c -> c.getResultStatus() == ResultStatus.FAILED).count();
        int blocked = (int) runCases.stream().filter(c -> c.getResultStatus() == ResultStatus.BLOCKED).count();
        int untested = (int) runCases.stream().filter(c -> c.getResultStatus() == ResultStatus.UNTESTED).count();

        List<TestRunCaseDto> caseDtos = null;
        if (includeCases) {
            caseDtos = runCases.stream().map(this::mapCaseToDto).collect(Collectors.toList());
        }

        return new TestRunDto(
                run.getId(),
                run.getProject().getId(),
                run.getMilestone() != null ? run.getMilestone().getId() : null,
                run.getMilestone() != null ? run.getMilestone().getName() : null,
                run.getName(),
                run.getStatus(),
                run.getCreatedBy() != null ? run.getCreatedBy().getId() : null,
                run.getCreatedBy() != null ? run.getCreatedBy().getFullName() : null,
                run.getCreatedAt(),
                run.getClosedAt(),
                total,
                passed,
                failed,
                blocked,
                untested,
                caseDtos
        );
    }

    private TestRunCaseDto mapCaseToDto(TestRunCase c) {
        String code = String.format("TC-%04d", c.getCaseId());
        return new TestRunCaseDto(
                c.getId(),
                c.getRun().getId(),
                c.getCaseId(),
                code,
                c.getTitle(),
                c.getPrecondition(),
                c.getSteps(),
                c.getExpectedResult(),
                c.getTestData(),
                c.getAssignedTo() != null ? c.getAssignedTo().getId() : null,
                c.getAssignedTo() != null ? c.getAssignedTo().getFullName() : null,
                c.getResultStatus(),
                c.getExecutedBy(),
                c.getExecutedAt(),
                c.getComment(),
                c.getDefectRef(),
                c.getIsReviewed(),
                c.getReviewedBy() != null ? c.getReviewedBy().getId() : null,
                c.getReviewedBy() != null ? c.getReviewedBy().getFullName() : null,
                c.getReviewedAt(),
                c.getReviewComment(),
                null
        );
    }
}
