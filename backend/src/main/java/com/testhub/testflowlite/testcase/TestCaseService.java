package com.testhub.testflowlite.testcase;

import com.testhub.testflowlite.audit.AuditLogService;
import com.testhub.testflowlite.common.ConflictException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.project.ProjectMemberRepository;
import com.testhub.testflowlite.project.ProjectRepository;
import com.testhub.testflowlite.section.Section;
import com.testhub.testflowlite.section.SectionRepository;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final SectionRepository sectionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<TestCaseDto> getTestCases(Long projectId, TestCaseFilterRequest filters, Pageable pageable, String currentUsername) {
        verifyProjectAccess(projectId, currentUsername);

        Specification<TestCase> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("section").get("project").get("id"), projectId));

            if (filters.getSectionId() != null) {
                predicates.add(cb.equal(root.get("section").get("id"), filters.getSectionId()));
            }
            if (filters.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), filters.getPriority()));
            }
            if (filters.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filters.getType()));
            }
            if (filters.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filters.getStatus()));
            }
            if (filters.getAutomationStatus() != null) {
                predicates.add(cb.equal(root.get("automationStatus"), filters.getAutomationStatus()));
            }
            if (filters.getKeyword() != null && !filters.getKeyword().trim().isEmpty()) {
                String kw = "%" + filters.getKeyword().trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), kw);
                Predicate codeLike = cb.like(cb.lower(root.get("code")), kw);
                predicates.add(cb.or(titleLike, codeLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<TestCase> page = testCaseRepository.findAll(spec, pageable);
        return page.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public TestCaseDto getTestCaseById(Long caseId, String currentUsername) {
        TestCase testCase = testCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + caseId));

        verifyProjectAccess(testCase.getSection().getProject().getId(), currentUsername);
        return mapToDto(testCase);
    }

    @Transactional
    public TestCaseDto createTestCase(Long projectId, CreateTestCaseRequest request, String currentUsername) {
        User currentUser = verifyProjectAccess(projectId, currentUsername);

        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + request.getSectionId()));

        if (!section.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Section does not belong to project id: " + projectId);
        }

        TestCase testCase = new TestCase();
        testCase.setSection(section);
        testCase.setTitle(request.getTitle());
        testCase.setPrecondition(request.getPrecondition());
        testCase.setSteps(request.getSteps());
        testCase.setExpectedResult(request.getExpectedResult());
        testCase.setTestData(request.getTestData());
        testCase.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        testCase.setType(request.getType() != null ? request.getType() : TestCaseType.FUNCTIONAL);
        testCase.setAutomationStatus(request.getAutomationStatus() != null ? request.getAutomationStatus() : AutomationStatus.MANUAL);
        testCase.setStatus(TestCaseStatus.DRAFT);
        testCase.setCreatedBy(currentUser);

        TestCase saved = testCaseRepository.save(testCase);
        saved.setCode(String.format("TC-%04d", saved.getId()));
        saved = testCaseRepository.save(saved);

        auditLogService.logAction(currentUser.getId(), "CREATE_TEST_CASE", "TEST_CASE", saved.getId(), "Created test case: " + saved.getCode());
        return mapToDto(saved);
    }

    @Transactional
    public TestCaseDto updateTestCase(Long caseId, UpdateTestCaseRequest request, String currentUsername) {
        TestCase testCase = testCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + caseId));

        Long projectId = testCase.getSection().getProject().getId();
        User currentUser = verifyProjectAccess(projectId, currentUsername);

        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + request.getSectionId()));

        if (!section.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Section does not belong to project id: " + projectId);
        }

        // Rule 14 & 15 Check
        if (currentUser.getRole() == Role.LEADER) {
            // Leader can edit any case anytime. If Ready, keeps Ready.
        } else {
            // Tester permissions check
            if (testCase.getCreatedBy() == null || !testCase.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have permission to edit this test case (not owner)");
            }
            if (testCase.getStatus() == TestCaseStatus.REVIEW) {
                throw new ConflictException("Cannot edit test case while pending review. Wait for Leader's decision.");
            }
            if (testCase.getStatus() == TestCaseStatus.READY) {
                testCase.setStatus(TestCaseStatus.DRAFT);
                auditLogService.logAction(currentUser.getId(), "REVERT_TEST_CASE_DRAFT", "TEST_CASE", caseId, "Reverted Ready test case to Draft upon edit by creator");
            }
        }

        testCase.setSection(section);
        testCase.setTitle(request.getTitle());
        testCase.setPrecondition(request.getPrecondition());
        testCase.setSteps(request.getSteps());
        testCase.setExpectedResult(request.getExpectedResult());
        testCase.setTestData(request.getTestData());
        testCase.setPriority(request.getPriority());
        testCase.setType(request.getType());
        testCase.setAutomationStatus(request.getAutomationStatus());

        TestCase updated = testCaseRepository.save(testCase);
        auditLogService.logAction(currentUser.getId(), "UPDATE_TEST_CASE", "TEST_CASE", updated.getId(), "Updated test case: " + updated.getCode());
        return mapToDto(updated);
    }

    @Transactional
    public void deleteTestCase(Long caseId, String currentUsername) {
        TestCase testCase = testCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + caseId));

        Long projectId = testCase.getSection().getProject().getId();
        User currentUser = verifyProjectAccess(projectId, currentUsername);

        if (currentUser.getRole() != Role.LEADER) {
            if (testCase.getCreatedBy() == null || !testCase.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have permission to delete this test case (not owner)");
            }
            if (testCase.getStatus() != TestCaseStatus.DRAFT) {
                throw new ConflictException("Cannot delete test case while in " + testCase.getStatus() + " status");
            }
        }

        testCaseRepository.delete(testCase);
        auditLogService.logAction(currentUser.getId(), "DELETE_TEST_CASE", "TEST_CASE", caseId, "Deleted test case: " + testCase.getCode());
    }

    @Transactional
    public TestCaseDto submitForReview(Long caseId, String currentUsername) {
        TestCase testCase = testCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + caseId));

        Long projectId = testCase.getSection().getProject().getId();
        User currentUser = verifyProjectAccess(projectId, currentUsername);

        if (currentUser.getRole() != Role.LEADER && (testCase.getCreatedBy() == null || !testCase.getCreatedBy().getId().equals(currentUser.getId()))) {
            throw new AccessDeniedException("Only the creator can submit this test case for review");
        }

        if (testCase.getStatus() != TestCaseStatus.DRAFT) {
            throw new ConflictException("Cannot submit test case for review from status " + testCase.getStatus() + ". Must be in DRAFT status.");
        }

        testCase.setStatus(TestCaseStatus.REVIEW);
        TestCase saved = testCaseRepository.save(testCase);

        auditLogService.logAction(currentUser.getId(), "SUBMIT_TEST_CASE", "TEST_CASE", caseId, "Submitted test case for review: " + saved.getCode());
        return mapToDto(saved);
    }

    @Transactional
    public TestCaseDto approveTestCase(Long caseId, String currentUsername) {
        TestCase testCase = testCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + caseId));

        User currentUser = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        if (currentUser.getRole() != Role.LEADER) {
            throw new AccessDeniedException("Only Leader can approve test cases");
        }

        if (testCase.getStatus() != TestCaseStatus.REVIEW) {
            throw new ConflictException("Cannot approve test case from status " + testCase.getStatus() + ". Must be in REVIEW status.");
        }

        testCase.setStatus(TestCaseStatus.READY);
        testCase.setReviewedBy(currentUser);
        testCase.setReviewedAt(LocalDateTime.now());
        TestCase saved = testCaseRepository.save(testCase);

        auditLogService.logAction(currentUser.getId(), "APPROVE_TEST_CASE", "TEST_CASE", caseId, "Approved test case: " + saved.getCode());
        return mapToDto(saved);
    }

    @Transactional
    public TestCaseDto rejectTestCase(Long caseId, RejectTestCaseRequest request, String currentUsername) {
        TestCase testCase = testCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + caseId));

        User currentUser = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        if (currentUser.getRole() != Role.LEADER) {
            throw new AccessDeniedException("Only Leader can reject test cases");
        }

        if (testCase.getStatus() != TestCaseStatus.REVIEW) {
            throw new ConflictException("Cannot reject test case from status " + testCase.getStatus() + ". Must be in REVIEW status.");
        }

        testCase.setStatus(TestCaseStatus.DRAFT);
        testCase.setReviewComment(request.getReviewComment());
        testCase.setReviewedBy(currentUser);
        testCase.setReviewedAt(LocalDateTime.now());
        TestCase saved = testCaseRepository.save(testCase);

        auditLogService.logAction(currentUser.getId(), "REJECT_TEST_CASE", "TEST_CASE", caseId, "Rejected test case: " + saved.getCode() + " with comment");
        return mapToDto(saved);
    }

    @Transactional
    public TestCaseDto cloneTestCase(Long caseId, String currentUsername) {
        TestCase original = testCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + caseId));

        Long projectId = original.getSection().getProject().getId();
        User currentUser = verifyProjectAccess(projectId, currentUsername);

        TestCase copy = new TestCase();
        copy.setSection(original.getSection());
        copy.setTitle(original.getTitle() + " (Copy)");
        copy.setPrecondition(original.getPrecondition());
        copy.setSteps(original.getSteps());
        copy.setExpectedResult(original.getExpectedResult());
        copy.setTestData(original.getTestData());
        copy.setPriority(original.getPriority());
        copy.setType(original.getType());
        copy.setAutomationStatus(original.getAutomationStatus());
        copy.setStatus(TestCaseStatus.DRAFT);
        copy.setCreatedBy(currentUser);

        TestCase saved = testCaseRepository.save(copy);
        saved.setCode(String.format("TC-%04d", saved.getId()));
        saved = testCaseRepository.save(saved);

        auditLogService.logAction(currentUser.getId(), "CLONE_TEST_CASE", "TEST_CASE", saved.getId(), "Cloned test case " + original.getCode() + " to new case: " + saved.getCode());
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<TestCaseDto> getReviewQueue(String currentUsername) {
        User currentUser = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        if (currentUser.getRole() != Role.LEADER) {
            throw new AccessDeniedException("Only Leader can view the review queue");
        }

        List<TestCase> reviewCases = testCaseRepository.findByStatusOrderByCreatedAtAsc(TestCaseStatus.REVIEW);
        return reviewCases.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private User verifyProjectAccess(Long projectId, String currentUsername) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        User user = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        if (user.getRole() != Role.LEADER && !projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new AccessDeniedException("You do not have access to this project");
        }

        return user;
    }

    private TestCaseDto mapToDto(TestCase tc) {
        Long projectId = tc.getSection() != null && tc.getSection().getProject() != null ? tc.getSection().getProject().getId() : null;
        Long sectionId = tc.getSection() != null ? tc.getSection().getId() : null;
        String sectionName = tc.getSection() != null ? tc.getSection().getName() : null;

        Long createdById = tc.getCreatedBy() != null ? tc.getCreatedBy().getId() : null;
        String createdByName = tc.getCreatedBy() != null ? tc.getCreatedBy().getFullName() : null;

        Long reviewedById = tc.getReviewedBy() != null ? tc.getReviewedBy().getId() : null;
        String reviewedByName = tc.getReviewedBy() != null ? tc.getReviewedBy().getFullName() : null;

        return new TestCaseDto(
                tc.getId(),
                tc.getCode(),
                projectId,
                sectionId,
                sectionName,
                tc.getTitle(),
                tc.getPrecondition(),
                tc.getSteps(),
                tc.getExpectedResult(),
                tc.getTestData(),
                tc.getPriority(),
                tc.getType(),
                tc.getAutomationStatus(),
                tc.getStatus(),
                tc.getReviewComment(),
                createdById,
                createdByName,
                reviewedById,
                reviewedByName,
                tc.getReviewedAt(),
                tc.getCreatedAt(),
                tc.getUpdatedAt()
        );
    }
}
