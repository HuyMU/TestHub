package com.testhub.testflowlite.execution;

import com.testhub.testflowlite.testrun.ResultStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionHistoryDto {
    private Long id;
    private Long runCaseId;
    private ResultStatus resultStatus;
    private String comment;
    private String executedBy;
    private LocalDateTime executedAt;
}
