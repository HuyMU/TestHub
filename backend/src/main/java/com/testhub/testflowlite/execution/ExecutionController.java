package com.testhub.testflowlite.execution;

import com.testhub.testflowlite.common.ApiResponse;
import com.testhub.testflowlite.testrun.TestRunCaseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/runs/{runId}/cases/{caseId}")
@RequiredArgsConstructor
@Tag(name = "Test Execution", description = "Execute test cases and record results/reviews")
public class ExecutionController {

    private final ExecutionService executionService;

    @PostMapping("/execute")
    @Operation(summary = "Record Execution Result (Passed/Failed/Blocked/Retest/Untested)")
    public ApiResponse<TestRunCaseDto> executeCase(
            @PathVariable Long runId,
            @PathVariable Long caseId,
            @RequestBody ExecutionDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        TestRunCaseDto result = executionService.recordExecution(runId, caseId, dto, userDetails.getUsername());
        return ApiResponse.success("Result recorded successfully", result);
    }

    @PostMapping("/review")
    @Operation(summary = "Leader Review Result (Reviewed / Request Retest)")
    public ApiResponse<TestRunCaseDto> reviewResult(
            @PathVariable Long runId,
            @PathVariable Long caseId,
            @RequestParam boolean reviewed,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal UserDetails userDetails) {
        TestRunCaseDto result = executionService.reviewResult(runId, caseId, reviewed, comment, userDetails.getUsername());
        return ApiResponse.success("Result review recorded successfully", result);
    }

    @GetMapping("/history")
    @Operation(summary = "Get Execution History for a Case in Test Run")
    public ApiResponse<List<ExecutionHistoryDto>> getHistory(
            @PathVariable Long runId,
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ExecutionHistoryDto> history = executionService.getExecutionHistory(runId, caseId, userDetails.getUsername());
        return ApiResponse.success("Execution history retrieved", history);
    }
}
