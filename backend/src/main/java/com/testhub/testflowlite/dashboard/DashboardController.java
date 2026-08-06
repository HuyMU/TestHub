package com.testhub.testflowlite.dashboard;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard & Reporting", description = "Project metrics and pass/fail/blocked statistics")
public class DashboardController {

    @GetMapping("/{projectId}")
    @Operation(summary = "Get Dashboard Summary Metrics")
    public ApiResponse<DashboardDto> getDashboardSummary(@PathVariable Long projectId) {
        // TODO: Implement dashboard metrics aggregation
        return ApiResponse.success(new DashboardDto(projectId, 0, 0, 0, 0, 0, 0));
    }
}
