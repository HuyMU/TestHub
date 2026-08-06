package com.testhub.testflowlite.milestone;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/milestones")
@RequiredArgsConstructor
@Tag(name = "Milestone Management", description = "Simple label with due dates attached to Projects")
public class MilestoneController {

    @GetMapping
    @Operation(summary = "List Milestones")
    public ApiResponse<List<MilestoneDto>> getMilestones() {
        return ApiResponse.success(Collections.emptyList());
    }

    @PostMapping
    @Operation(summary = "Create Milestone (Leader only)")
    public ApiResponse<MilestoneDto> createMilestone(@RequestBody MilestoneDto dto) {
        return ApiResponse.success("Milestone created", dto);
    }
}
