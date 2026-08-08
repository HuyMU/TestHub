package com.testhub.testflowlite.testcase;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Test Case Management", description = "Endpoints for managing test cases and review workflow")
@SecurityRequirement(name = "bearerAuth")
public class TestCaseController {

    private final TestCaseService testCaseService;

    @GetMapping("/projects/{projectId}/cases")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get list of test cases in project with filters and pagination")
    public ApiResponse<Page<TestCaseDto>> getTestCases(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) TestCaseType type,
            @RequestParam(required = false) TestCaseStatus status,
            @RequestParam(required = false) AutomationStatus automationStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort,
            @AuthenticationPrincipal UserDetails userDetails) {

        Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));

        TestCaseFilterRequest filters = new TestCaseFilterRequest(sectionId, priority, type, status, automationStatus, keyword);
        Page<TestCaseDto> result = testCaseService.getTestCases(projectId, filters, pageable, userDetails.getUsername());
        return ApiResponse.success(result);
    }

    @GetMapping("/cases/review-queue")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Get global review queue (Leader only)")
    public ApiResponse<List<TestCaseDto>> getReviewQueue(@AuthenticationPrincipal UserDetails userDetails) {
        List<TestCaseDto> reviewQueue = testCaseService.getReviewQueue(userDetails.getUsername());
        return ApiResponse.success(reviewQueue);
    }

    @GetMapping("/cases/{caseId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get test case details by ID")
    public ApiResponse<TestCaseDto> getTestCaseById(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        TestCaseDto dto = testCaseService.getTestCaseById(caseId, userDetails.getUsername());
        return ApiResponse.success(dto);
    }

    @PostMapping("/projects/{projectId}/cases")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new test case in project")
    public ApiResponse<TestCaseDto> createTestCase(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTestCaseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        TestCaseDto created = testCaseService.createTestCase(projectId, request, userDetails.getUsername());
        return ApiResponse.success(created);
    }

    @PutMapping("/cases/{caseId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update an existing test case")
    public ApiResponse<TestCaseDto> updateTestCase(
            @PathVariable Long caseId,
            @Valid @RequestBody UpdateTestCaseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        TestCaseDto updated = testCaseService.updateTestCase(caseId, request, userDetails.getUsername());
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/cases/{caseId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a test case")
    public ApiResponse<Void> deleteTestCase(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        testCaseService.deleteTestCase(caseId, userDetails.getUsername());
        return ApiResponse.success(null);
    }

    @PostMapping("/cases/{caseId}/submit-review")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit test case for review (Draft -> Review)")
    public ApiResponse<TestCaseDto> submitForReview(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        TestCaseDto submitted = testCaseService.submitForReview(caseId, userDetails.getUsername());
        return ApiResponse.success(submitted);
    }

    @PostMapping("/cases/{caseId}/approve")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Approve test case (Review -> Ready, Leader only)")
    public ApiResponse<TestCaseDto> approveTestCase(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        TestCaseDto approved = testCaseService.approveTestCase(caseId, userDetails.getUsername());
        return ApiResponse.success(approved);
    }

    @PostMapping("/cases/{caseId}/reject")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Reject test case with comment (Review -> Draft, Leader only)")
    public ApiResponse<TestCaseDto> rejectTestCase(
            @PathVariable Long caseId,
            @Valid @RequestBody RejectTestCaseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        TestCaseDto rejected = testCaseService.rejectTestCase(caseId, request, userDetails.getUsername());
        return ApiResponse.success(rejected);
    }

    @PostMapping("/cases/{caseId}/clone")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Clone/duplicate test case")
    public ApiResponse<TestCaseDto> cloneTestCase(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        TestCaseDto cloned = testCaseService.cloneTestCase(caseId, userDetails.getUsername());
        return ApiResponse.success(cloned);
    }
}
