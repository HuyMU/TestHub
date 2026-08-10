package com.testhub.testflowlite.automation;

import com.testhub.testflowlite.apitoken.ApiToken;
import com.testhub.testflowlite.apitoken.ApiTokenRepository;
import com.testhub.testflowlite.apitoken.ApiTokenService;
import com.testhub.testflowlite.audit.AuditLogService;
import com.testhub.testflowlite.common.BadRequestException;
import com.testhub.testflowlite.common.ConflictException;
import com.testhub.testflowlite.common.InvalidCredentialsException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.execution.ExecutionHistory;
import com.testhub.testflowlite.execution.ExecutionHistoryRepository;
import com.testhub.testflowlite.testcase.TestCase;
import com.testhub.testflowlite.testcase.TestCaseRepository;
import com.testhub.testflowlite.testrun.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AutomationService {

    private final ApiTokenRepository apiTokenRepository;
    private final ApiTokenService apiTokenService;
    private final TestRunRepository testRunRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestRunCaseRepository testRunCaseRepository;
    private final ExecutionHistoryRepository executionHistoryRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void submitResult(String rawApiToken, AutomationResultDto dto) {
        if (rawApiToken == null || rawApiToken.trim().isEmpty()) {
            throw new InvalidCredentialsException("Invalid or revoked API token");
        }

        String tokenHash = apiTokenService.hashToken(rawApiToken.trim());
        ApiToken apiToken = apiTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or revoked API token"));

        if (apiToken.getRevokedAt() != null) {
            throw new InvalidCredentialsException("Invalid or revoked API token");
        }

        apiToken.setLastUsedAt(LocalDateTime.now());
        apiTokenRepository.save(apiToken);

        if (dto.getRunId() == null) {
            throw new BadRequestException("run_id is required");
        }

        if (dto.getCaseRef() == null || dto.getCaseRef().trim().isEmpty()) {
            throw new BadRequestException("case_ref is required");
        }

        TestRun run = testRunRepository.findById(dto.getRunId())
                .orElseThrow(() -> new ResourceNotFoundException("Test Run not found: " + dto.getRunId()));

        TestCase testCase = testCaseRepository.findByCode(dto.getCaseRef().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Test Case not found with code: " + dto.getCaseRef()));

        TestRunCase runCase = testRunCaseRepository.findByRunIdAndCaseId(run.getId(), testCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Test Case " + dto.getCaseRef() + " is not in Test Run #" + run.getId()));

        if (run.getStatus() == RunStatus.CLOSED) {
            throw new ConflictException("Cannot record execution on a closed Test Run");
        }

        if (dto.getStatus() == null || dto.getStatus().trim().isEmpty()) {
            throw new BadRequestException("status is required");
        }

        ResultStatus statusToSet;
        try {
            statusToSet = ResultStatus.valueOf(dto.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid result status: " + dto.getStatus() + ". Valid values are: PASSED, FAILED, BLOCKED, RETEST, UNTESTED.");
        }

        String executedByStr = "Automation (token #" + apiToken.getId() + ")";
        LocalDateTime execTime = dto.getExecutedAt() != null ? dto.getExecutedAt() : LocalDateTime.now();

        runCase.setResultStatus(statusToSet);
        runCase.setComment(dto.getMessage());
        runCase.setExecutedBy(executedByStr);
        runCase.setExecutedAt(execTime);
        runCase.setIsReviewed(false);
        testRunCaseRepository.save(runCase);

        ExecutionHistory history = new ExecutionHistory();
        history.setRunCase(runCase);
        history.setResultStatus(statusToSet);
        history.setDurationMs(dto.getDurationMs());
        history.setComment(dto.getMessage());
        history.setExecutedBy(executedByStr);
        history.setExecutedAt(execTime);
        executionHistoryRepository.save(history);

        auditLogService.logAction(apiToken.getCreatedBy().getId(), "AUTOMATION_SUBMIT_RESULT", "TEST_RUN_CASE", runCase.getId(),
                "Automated result ingested via token #" + apiToken.getId() + ": " + statusToSet);
    }
}
