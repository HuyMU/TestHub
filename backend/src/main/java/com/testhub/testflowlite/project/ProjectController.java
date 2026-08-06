package com.testhub.testflowlite.project;

import com.testhub.testflowlite.common.ApiResponse;
import com.testhub.testflowlite.user.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Project Management", description = "Project CRUD and Member assignments")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List visible Projects (Leader sees all, Tester sees assigned)")
    public ApiResponse<List<ProjectDto>> getProjects(@AuthenticationPrincipal UserDetails currentUser) {
        return ApiResponse.success(projectService.getAllProjects(currentUser.getUsername()));
    }

    @PostMapping
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Create a new Project (Leader only)")
    public ApiResponse<ProjectDto> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ApiResponse.success("Project created successfully", projectService.createProject(request, currentUser.getUsername()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Project by ID")
    public ApiResponse<ProjectDto> getProjectById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ApiResponse.success(projectService.getProjectById(id, currentUser.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Update Project details or status (Leader only)")
    public ApiResponse<ProjectDto> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ApiResponse.success("Project updated successfully", projectService.updateProject(id, request, currentUser.getUsername()));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Assign Testers to Project (Leader only)")
    public ApiResponse<Void> assignMembers(
            @PathVariable Long id,
            @Valid @RequestBody AssignMembersRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        projectService.assignMembers(id, request, currentUser.getUsername());
        return ApiResponse.success("Members assigned successfully", null);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Remove Tester from Project (Leader only)")
    public ApiResponse<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails currentUser) {
        projectService.removeMember(id, userId, currentUser.getUsername());
        return ApiResponse.success("Member removed successfully", null);
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List Testers assigned to Project")
    public ApiResponse<List<UserDto>> getProjectMembers(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ApiResponse.success(projectService.getProjectMembers(id, currentUser.getUsername()));
    }
}
