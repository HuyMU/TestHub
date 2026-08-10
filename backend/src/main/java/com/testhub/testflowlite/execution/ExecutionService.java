package com.testhub.testflowlite.execution;

import com.testhub.testflowlite.audit.AuditLogService;
import com.testhub.testflowlite.common.BadRequestException;
import com.testhub.testflowlite.common.ConflictException;
import com.testhub.testflowlite.common.ForbiddenException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.testcase.TestCase;
import com.testhub.testflowlite.testcase.TestCaseRepository;
import com.testhub.testflowlite.testrun.*;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final TestRunCaseRepository testRunCaseRepository;
    private final ExecutionHistoryRepository executionHistoryRepository;
    private final TestCaseRepository testCaseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public TestRunCaseDto recordExecution(Long runId, Long caseId, ExecutionDto dto, String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        TestRunCase runCase = testRunCaseRepository.findByRunIdAndCaseId(runId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found in Test Run: " + caseId));

        if (runCase.getRun().getStatus() == RunStatus.CLOSED) {
            throw new ConflictException("Cannot record execution on a closed Test Run");
        }

        if (user.getRole() != Role.LEADER) {
            if (runCase.getAssignedTo() == null || !runCase.getAssignedTo().getId().equals(user.getId())) {
                throw new ForbiddenException("You are not assigned to execute this test case");
            }
        }

        ResultStatus statusToSet = dto.getResultStatus() != null ? dto.getResultStatus() : ResultStatus.UNTESTED;
        runCase.setResultStatus(statusToSet);
        runCase.setComment(dto.getComment());
        runCase.setDefectRef(dto.getDefectRef());
        runCase.setExecutedBy(user.getFullName() != null ? user.getFullName() : user.getUsername());
        runCase.setExecutedAt(LocalDateTime.now());
        runCase.setIsReviewed(false);

        testRunCaseRepository.save(runCase);

        ExecutionHistory history = new ExecutionHistory();
        history.setRunCase(runCase);
        history.setResultStatus(statusToSet);
        history.setComment(dto.getComment());
        history.setExecutedBy(user.getFullName() != null ? user.getFullName() : user.getUsername());
        history.setExecutedAt(LocalDateTime.now());
        executionHistoryRepository.save(history);

        auditLogService.logAction(user.getId(), "EXECUTE_TEST_CASE", "TEST_RUN_CASE", runCase.getId(),
                "Executed test case in run with result: " + statusToSet);

        return mapToDto(runCase);
    }

    @Transactional
    public TestRunCaseDto reviewResult(Long runId, Long caseId, boolean reviewed, String comment, String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        if (user.getRole() != Role.LEADER) {
            throw new ForbiddenException("Only Leaders can review execution results");
        }

        TestRunCase runCase = testRunCaseRepository.findByRunIdAndCaseId(runId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found in Test Run: " + caseId));

        if (runCase.getResultStatus() == ResultStatus.UNTESTED) {
            throw new BadRequestException("Case has not been executed yet");
        }

        if (reviewed) {
            runCase.setIsReviewed(true);
            runCase.setReviewedBy(user);
            runCase.setReviewedAt(LocalDateTime.now());
            runCase.setReviewComment(comment);
        } else {
            if (comment == null || comment.trim().isEmpty()) {
                throw new BadRequestException("Comment is required when requesting retest");
            }
            runCase.setResultStatus(ResultStatus.RETEST);
            runCase.setIsReviewed(false);
            runCase.setReviewedBy(null);
            runCase.setReviewedAt(null);
            runCase.setReviewComment(comment);
        }

        testRunCaseRepository.save(runCase);

        auditLogService.logAction(user.getId(), "REVIEW_TEST_RESULT", "TEST_RUN_CASE", runCase.getId(),
                "Reviewed result: " + (reviewed ? "APPROVED" : "RETEST_REQUESTED"));

        return mapToDto(runCase);
    }

    @Transactional(readOnly = true)
    public List<ExecutionHistoryDto> getExecutionHistory(Long runId, Long caseId, String currentUsername) {
        testRunCaseRepository.findByRunIdAndCaseId(runId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found in Test Run: " + caseId));

        List<ExecutionHistory> histories = executionHistoryRepository
                .findByRunCaseRunIdAndRunCaseCaseIdOrderByExecutedAtDesc(runId, caseId);

        return histories.stream().map(h -> new ExecutionHistoryDto(
                h.getId(),
                h.getRunCase().getId(),
                h.getResultStatus(),
                h.getComment(),
                h.getExecutedBy(),
                h.getExecutedAt()
        )).collect(Collectors.toList());
    }

    private TestRunCaseDto mapToDto(TestRunCase trc) {
        String code = testCaseRepository.findById(trc.getCaseId())
                .map(TestCase::getCode)
                .orElse("TC-" + trc.getCaseId());

        return new TestRunCaseDto(
                trc.getId(),
                trc.getRun().getId(),
                trc.getCaseId(),
                code,
                trc.getTitle(),
                trc.getPrecondition(),
                trc.getSteps(),
                trc.getExpectedResult(),
                trc.getTestData(),
                trc.getAssignedTo() != null ? trc.getAssignedTo().getId() : null,
                trc.getAssignedTo() != null ? trc.getAssignedTo().getFullName() : null,
                trc.getResultStatus(),
                trc.getExecutedBy(),
                trc.getExecutedAt(),
                trc.getComment(),
                trc.getDefectRef(),
                trc.getIsReviewed(),
                trc.getReviewedBy() != null ? trc.getReviewedBy().getId() : null,
                trc.getReviewedBy() != null ? trc.getReviewedBy().getFullName() : null,
                trc.getReviewedAt(),
                trc.getReviewComment()
        );
    }
}
