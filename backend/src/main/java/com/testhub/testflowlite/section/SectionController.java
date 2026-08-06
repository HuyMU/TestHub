package com.testhub.testflowlite.section;

import com.testhub.testflowlite.common.ApiResponse;
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
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Section & Subsection", description = "Hierarchical section management")
public class SectionController {

    private final SectionService sectionService;

    @GetMapping("/api/projects/{projectId}/sections")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Section Hierarchy Tree for Project")
    public ApiResponse<List<SectionDto>> getSectionTree(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ApiResponse.success(sectionService.getSectionTree(projectId, currentUser.getUsername()));
    }

    @PostMapping("/api/projects/{projectId}/sections")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create Section or Subsection (Leader or assigned Tester)")
    public ApiResponse<SectionDto> createSection(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateSectionRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ApiResponse.success("Section created successfully", sectionService.createSection(projectId, request, currentUser.getUsername()));
    }

    @PutMapping("/api/sections/{sectionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update Section name or parent (Leader or assigned Tester)")
    public ApiResponse<SectionDto> updateSection(
            @PathVariable Long sectionId,
            @Valid @RequestBody UpdateSectionRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ApiResponse.success("Section updated successfully", sectionService.updateSection(sectionId, request, currentUser.getUsername()));
    }

    @PutMapping("/api/projects/{projectId}/sections/reorder")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reorder Sections batch (Leader or assigned Tester)")
    public ApiResponse<Void> reorderSections(
            @PathVariable Long projectId,
            @Valid @RequestBody ReorderSectionsRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        sectionService.reorderSections(projectId, request, currentUser.getUsername());
        return ApiResponse.success("Sections reordered successfully", null);
    }

    @DeleteMapping("/api/sections/{sectionId}")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Delete empty Section (Leader only - returns 409 Conflict if non-empty)")
    public ApiResponse<Void> deleteSection(
            @PathVariable Long sectionId,
            @AuthenticationPrincipal UserDetails currentUser) {
        sectionService.deleteSection(sectionId, currentUser.getUsername());
        return ApiResponse.success("Section deleted successfully", null);
    }
}
