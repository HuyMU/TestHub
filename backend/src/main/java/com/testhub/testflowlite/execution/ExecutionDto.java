package com.testhub.testflowlite.execution;

import com.testhub.testflowlite.testrun.ResultStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionDto {
    @NotNull(message = "Result status is required")
    private ResultStatus resultStatus;
    private String comment;
    private String defectRef;
}
