package com.testhub.testflowlite.automation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomationResultDto {
    @JsonProperty("run_id")
    private Long runId;

    @JsonProperty("case_ref")
    private String caseRef;

    private String status;

    @JsonProperty("duration_ms")
    private Long durationMs;

    private String message;

    @JsonProperty("executed_at")
    private LocalDateTime executedAt;
}
