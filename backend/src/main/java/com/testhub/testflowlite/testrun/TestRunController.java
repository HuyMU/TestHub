package com.testhub.testflowlite.testrun;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Test Run Management", description = "Test Run creation, case selection, snapshotting, and status management")
@SecurityRequirement(name = "bearerAuth")
public class TestRunController {

    private final TestRunService testRunService;

    @GetMapping("/api/projects/{projectId}/runs")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List Test Runs for Project")
    public ApiResponse<List<TestRunDto>> getTestRuns(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success(testRunService.getTestRuns(projectId, userDetails.getUsername()));
    }

    @PostMapping("/api/projects/{projectId}/runs")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Create Test Run (Leader only, selects Ready cases, snapshots content, assigns Testers)")
    public ApiResponse<TestRunDto> createTestRun(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTestRunRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success("Test Run created successfully", testRunService.createTestRun(projectId, request, userDetails.getUsername()));
    }

    @GetMapping("/api/runs/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Test Run details with snapshotted test cases")
    public ApiResponse<TestRunDto> getTestRunDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success(testRunService.getTestRunDetail(id, userDetails.getUsername()));
    }

    @PostMapping("/api/runs/{id}/cases")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Add test cases to open Test Run (Leader only)")
    public ApiResponse<TestRunDto> addCasesToRun(
            @PathVariable Long id,
            @Valid @RequestBody AddCasesToRunRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success("Cases added to Test Run", testRunService.addCasesToRun(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/api/runs/{id}/cases/{runCaseId}")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Remove a test case from open Test Run (Leader only)")
    public ApiResponse<Void> removeCaseFromRun(
            @PathVariable Long id,
            @PathVariable Long runCaseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        testRunService.removeCaseFromRun(id, runCaseId, userDetails.getUsername());
        return ApiResponse.success("Test Case removed from Test Run", null);
    }

    @PostMapping("/api/runs/{id}/close")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Close Test Run (Leader only, status=CLOSED, closed_at=now)")
    public ApiResponse<TestRunDto> closeTestRun(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success("Test Run closed successfully", testRunService.closeTestRun(id, userDetails.getUsername()));
    }

    @GetMapping("/api/runs/{id}/report")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get detailed Test Run execution report (FR-31)")
    public ApiResponse<TestRunReportDto> getTestRunReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        TestRunReportDto report = testRunService.generateReport(id, userDetails.getUsername());
        return ApiResponse.success("Test Run report generated successfully", report);
    }

    @GetMapping("/api/runs/{id}/report/export")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Export detailed Test Run report to formatted Excel sheet (FR-31)")
    public org.springframework.http.ResponseEntity<byte[]> exportTestRunReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        byte[] excelContent = testRunService.exportReportToExcel(id, userDetails.getUsername());
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=TestRun_Report_" + id + ".xlsx")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelContent);
    }
}
