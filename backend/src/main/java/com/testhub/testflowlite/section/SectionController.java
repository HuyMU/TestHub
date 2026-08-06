package com.testhub.testflowlite.section;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/sections")
@RequiredArgsConstructor
@Tag(name = "Section & Subsection", description = "Hierarchical section management")
public class SectionController {

    @GetMapping
    @Operation(summary = "Get Section Hierarchy Tree")
    public ApiResponse<List<SectionDto>> getSectionTree(@PathVariable Long projectId) {
        // TODO: Implement section tree retrieval
        return ApiResponse.success(Collections.emptyList());
    }

    @PostMapping
    @Operation(summary = "Create Section / Subsection")
    public ApiResponse<SectionDto> createSection(@PathVariable Long projectId, @RequestBody SectionDto dto) {
        // TODO: Implement section creation
        return ApiResponse.success("Section created", dto);
    }
}
