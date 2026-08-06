package com.testhub.testflowlite.testcase;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Tag(name = "Test Case Management", description = "Test Case CRUD and Workflow (Draft/Review/Ready)")
public class TestCaseController {

    @GetMapping
    @Operation(summary = "List Test Cases")
    public ApiResponse<List<TestCaseDto>> getTestCases() {
        // TODO: Implement test case search and filter
        return ApiResponse.success(Collections.emptyList());
    }

    @PostMapping
    @Operation(summary = "Create Test Case (Draft status)")
    public ApiResponse<TestCaseDto> createTestCase(@RequestBody TestCaseDto dto) {
        // TODO: Implement case creation
        return ApiResponse.success("Test Case created in Draft state", dto);
    }

    @PostMapping("/{id}/submit-review")
    @Operation(summary = "Submit Test Case for Leader Review (Draft -> Review)")
    public ApiResponse<TestCaseDto> submitForReview(@PathVariable Long id) {
        // TODO: Implement workflow submission
        return ApiResponse.success("Case submitted for review", new TestCaseDto());
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Leader Approve Test Case (Review -> Ready)")
    public ApiResponse<TestCaseDto> approveCase(@PathVariable Long id) {
        // TODO: Implement Leader approval
        return ApiResponse.success("Case approved to Ready status", new TestCaseDto());
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Leader Reject Test Case (Review -> Draft)")
    public ApiResponse<TestCaseDto> rejectCase(@PathVariable Long id, @RequestParam String comment) {
        // TODO: Implement Leader rejection
        return ApiResponse.success("Case returned to Draft status", new TestCaseDto());
    }

    @GetMapping("/review-queue")
    @Operation(summary = "Review Queue (Leader)")
    public ApiResponse<List<TestCaseDto>> getReviewQueue() {
        // TODO: Implement review queue query
        return ApiResponse.success(Collections.emptyList());
    }
}
