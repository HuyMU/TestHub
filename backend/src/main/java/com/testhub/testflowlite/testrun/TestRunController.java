package com.testhub.testflowlite.testrun;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/runs")
@RequiredArgsConstructor
@Tag(name = "Test Run Management", description = "Test Run creation and case assignment")
public class TestRunController {

    @GetMapping
    @Operation(summary = "List Test Runs")
    public ApiResponse<List<TestRunDto>> getTestRuns() {
        return ApiResponse.success(Collections.emptyList());
    }

    @PostMapping
    @Operation(summary = "Create Test Run (Leader only)")
    public ApiResponse<TestRunDto> createTestRun(@RequestBody TestRunDto dto) {
        return ApiResponse.success("Test Run created", dto);
    }
}
