package com.testhub.testflowlite.execution;

import com.testhub.testflowlite.common.ResultStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionDto {
    private ResultStatus resultStatus;
    private String comment;
    private String defectRef;
}
