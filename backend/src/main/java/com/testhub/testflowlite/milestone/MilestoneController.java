package com.testhub.testflowlite.milestone;

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
@RequestMapping("/api/projects/{projectId}/milestones")
@RequiredArgsConstructor
@Tag(name = "Milestone Management", description = "Milestone CRUD operations for Projects")
@SecurityRequirement(name = "bearerAuth")
public class MilestoneController {

    private final MilestoneService milestoneService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List Milestones for Project")
    public ApiResponse<List<MilestoneDto>> getMilestones(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success(milestoneService.getMilestones(projectId, userDetails.getUsername()));
    }

    @PostMapping
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Create Milestone (Leader only)")
    public ApiResponse<MilestoneDto> createMilestone(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateMilestoneRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success("Milestone created successfully", milestoneService.createMilestone(projectId, request, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Update Milestone details / status (Leader only)")
    public ApiResponse<MilestoneDto> updateMilestone(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateMilestoneRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success("Milestone updated successfully", milestoneService.updateMilestone(projectId, id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Delete Milestone (Leader only, blocked if referenced by Test Runs)")
    public ApiResponse<Void> deleteMilestone(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        milestoneService.deleteMilestone(projectId, id, userDetails.getUsername());
        return ApiResponse.success("Milestone deleted successfully", null);
    }
}
