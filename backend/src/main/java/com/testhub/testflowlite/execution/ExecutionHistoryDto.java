package com.testhub.testflowlite.execution;

import com.testhub.testflowlite.attachment.AttachmentDto;
import com.testhub.testflowlite.testrun.ResultStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    private Long durationMs;
    private List<AttachmentDto> attachments;

    public ExecutionHistoryDto(Long id, Long runCaseId, ResultStatus resultStatus, String comment, String executedBy, LocalDateTime executedAt) {
        this(id, runCaseId, resultStatus, comment, executedBy, executedAt, null, null);
    }

    public ExecutionHistoryDto(Long id, Long runCaseId, ResultStatus resultStatus, String comment, String executedBy, LocalDateTime executedAt, Long durationMs) {
        this(id, runCaseId, resultStatus, comment, executedBy, executedAt, durationMs, null);
    }
}
