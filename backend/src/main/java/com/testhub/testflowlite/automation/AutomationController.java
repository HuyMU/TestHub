package com.testhub.testflowlite.automation;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/automation")
@RequiredArgsConstructor
@Tag(name = "Automation Results API", description = "Endpoint for ingestion of automated test results (API Token Auth)")
public class AutomationController {

    @PostMapping("/results")
    @Operation(summary = "Post Automated Test Result", security = @SecurityRequirement(name = "apiToken"))
    public ApiResponse<Void> submitResult(
            @RequestHeader(value = "X-API-TOKEN", required = true) String apiToken,
            @RequestBody AutomationResultDto dto) {
        // TODO: Implement API token validation & result ingestion
        return ApiResponse.success("Automated test result ingested successfully", null);
    }
}
