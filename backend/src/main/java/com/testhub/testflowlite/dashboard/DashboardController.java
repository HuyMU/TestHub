package com.testhub.testflowlite.dashboard;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard & Reporting", description = "Project metrics and pass/fail/blocked statistics")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Dashboard Summary Metrics")
    public ApiResponse<DashboardDto> getDashboardSummary(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        DashboardDto dto = dashboardService.getDashboard(projectId, userDetails.getUsername());
        return ApiResponse.success("Dashboard metrics retrieved successfully", dto);
    }
}
