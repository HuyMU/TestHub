package com.testhub.testflowlite.project;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Project Management", description = "Project CRUD and Member assignments")
public class ProjectController {

    @GetMapping
    @Operation(summary = "List Projects")
    public ApiResponse<List<ProjectDto>> getProjects() {
        // TODO: Implement project retrieval logic
        return ApiResponse.success(Collections.emptyList());
    }

    @PostMapping
    @Operation(summary = "Create Project (Leader only)")
    public ApiResponse<ProjectDto> createProject(@RequestBody ProjectDto dto) {
        // TODO: Implement project creation logic
        return ApiResponse.success("Project created", dto);
    }
}
