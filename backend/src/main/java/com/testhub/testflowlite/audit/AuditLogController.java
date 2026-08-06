package com.testhub.testflowlite.audit;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logging", description = "System event audit trail")
public class AuditLogController {

    @GetMapping
    @Operation(summary = "List Audit Logs")
    public ApiResponse<List<String>> getAuditLogs() {
        return ApiResponse.success(Collections.emptyList());
    }
}
