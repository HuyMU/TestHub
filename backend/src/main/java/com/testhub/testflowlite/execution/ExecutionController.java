package com.testhub.testflowlite.execution;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/runs/{runId}/cases/{caseId}")
@RequiredArgsConstructor
@Tag(name = "Test Execution", description = "Execute test cases and record results/reviews")
public class ExecutionController {

    @PostMapping("/execute")
    @Operation(summary = "Record Execution Result (Passed/Failed/Blocked/Retest/Untested)")
    public ApiResponse<Void> executeCase(
            @PathVariable Long runId,
            @PathVariable Long caseId,
            @RequestBody ExecutionDto dto) {
        // TODO: Implement execution recording
        return ApiResponse.success("Result recorded", null);
    }

    @PostMapping("/review")
    @Operation(summary = "Leader Review Result (Reviewed / Request Retest)")
    public ApiResponse<Void> reviewResult(
            @PathVariable Long runId,
            @PathVariable Long caseId,
            @RequestParam boolean reviewed,
            @RequestParam(required = false) String comment) {
        // TODO: Implement result review logic
        return ApiResponse.success("Result review recorded", null);
    }
}
