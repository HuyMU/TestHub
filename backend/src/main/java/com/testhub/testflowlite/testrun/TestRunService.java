package com.testhub.testflowlite.testrun;

import com.testhub.testflowlite.audit.AuditLogService;
import com.testhub.testflowlite.common.ConflictException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.milestone.Milestone;
import com.testhub.testflowlite.milestone.MilestoneRepository;
import com.testhub.testflowlite.project.Project;
import com.testhub.testflowlite.project.ProjectAccessGuard;
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
    private final ProjectAccessGuard projectAccessGuard;
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

                if (!projectAccessGuard.hasProjectAccess(projectId, assignedUser.getId(), assignedUser.getRole())) {
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

        return projectAccessGuard.verifyProjectAccess(projectId, currentUsername);
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

    @Transactional(readOnly = true)
    public TestRunReportDto generateReport(Long runId, String currentUsername) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Test Run not found: " + runId));
        verifyProjectAccess(run.getProject().getId(), currentUsername);

        List<TestRunCase> runCases = testRunCaseRepository.findByRunIdOrderByIdAsc(run.getId());
        long total = runCases.size();
        long passed = runCases.stream().filter(c -> c.getResultStatus() == ResultStatus.PASSED).count();
        long failed = runCases.stream().filter(c -> c.getResultStatus() == ResultStatus.FAILED).count();
        long blocked = runCases.stream().filter(c -> c.getResultStatus() == ResultStatus.BLOCKED).count();
        long retest = runCases.stream().filter(c -> c.getResultStatus() == ResultStatus.RETEST).count();
        long untested = runCases.stream().filter(c -> c.getResultStatus() == ResultStatus.UNTESTED).count();

        double passRate = total > 0 ? (passed * 100.0) / total : 0.0;
        long completed = total - untested;
        double completionRate = total > 0 ? (completed * 100.0) / total : 0.0;

        List<TestRunCaseReportDto> caseReports = runCases.stream().map(c -> new TestRunCaseReportDto(
                c.getCaseId(),
                String.format("TC-%04d", c.getCaseId()),
                c.getTitle(),
                c.getAssignedTo() != null ? c.getAssignedTo().getFullName() : "Unassigned",
                c.getResultStatus(),
                c.getExecutedBy() != null ? c.getExecutedBy() : "N/A",
                c.getExecutedAt(),
                c.getComment(),
                c.getDefectRef()
        )).collect(Collectors.toList());

        return new TestRunReportDto(
                run.getId(),
                run.getName(),
                run.getProject().getName(),
                run.getMilestone() != null ? run.getMilestone().getName() : "None",
                run.getStatus(),
                run.getClosedAt(),
                total,
                passed,
                failed,
                blocked,
                retest,
                untested,
                Math.round(passRate * 100.0) / 100.0,
                Math.round(completionRate * 100.0) / 100.0,
                caseReports
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportReportToExcel(Long runId, String currentUsername) {
        TestRunReportDto report = generateReport(runId, currentUsername);

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Run Report");

            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setFontName("Times New Roman");
            font.setFontHeightInPoints((short) 13);

            org.apache.poi.ss.usermodel.CellStyle normalStyle = workbook.createCellStyle();
            normalStyle.setFont(font);

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setFontName("Times New Roman");
            headerFont.setFontHeightInPoints((short) 13);
            headerFont.setBold(true);

            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            int rowNum = 0;

            // Summary Header Block
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("TEST RUN REPORT: " + report.getRunName());
            titleCell.setCellStyle(headerStyle);

            rowNum++; // blank row

            String[][] summaryData = {
                    {"Project:", report.getProjectName()},
                    {"Milestone:", report.getMilestoneName()},
                    {"Status:", report.getRunStatus().name()},
                    {"Total Cases:", String.valueOf(report.getTotalCases())},
                    {"Passed:", String.valueOf(report.getPassedCases())},
                    {"Failed:", String.valueOf(report.getFailedCases())},
                    {"Blocked:", String.valueOf(report.getBlockedCases())},
                    {"Retest:", String.valueOf(report.getRetestCases())},
                    {"Untested:", String.valueOf(report.getUntestedCases())},
                    {"Pass Rate:", report.getPassRatePercentage() + "%"},
                    {"Completion Rate:", report.getCompletionPercentage() + "%"}
            };

            for (String[] pair : summaryData) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                org.apache.poi.ss.usermodel.Cell kCell = row.createCell(0);
                kCell.setCellValue(pair[0]);
                kCell.setCellStyle(headerStyle);
                org.apache.poi.ss.usermodel.Cell vCell = row.createCell(1);
                vCell.setCellValue(pair[1]);
                vCell.setCellStyle(normalStyle);
            }

            rowNum++; // blank row

            // Details Table Header
            String[] headers = {"Case Code", "Title", "Assigned To", "Result Status", "Executed By", "Executed At", "Comment", "Defect Ref"};
            org.apache.poi.ss.usermodel.Row tableHeaderRow = sheet.createRow(rowNum++);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = tableHeaderRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // Details Table Data
            for (TestRunCaseReportDto tc : report.getCases()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(tc.getCode());
                row.createCell(1).setCellValue(tc.getTitle() != null ? tc.getTitle() : "");
                row.createCell(2).setCellValue(tc.getAssignedToName() != null ? tc.getAssignedToName() : "");
                row.createCell(3).setCellValue(tc.getResultStatus() != null ? tc.getResultStatus().name() : "");
                row.createCell(4).setCellValue(tc.getExecutedBy() != null ? tc.getExecutedBy() : "");
                row.createCell(5).setCellValue(tc.getExecutedAt() != null ? tc.getExecutedAt().toString() : "");
                row.createCell(6).setCellValue(tc.getComment() != null ? tc.getComment() : "");
                row.createCell(7).setCellValue(tc.getDefectRef() != null ? tc.getDefectRef() : "");

                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(normalStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to export Test Run report to Excel", e);
        }
    }
}
