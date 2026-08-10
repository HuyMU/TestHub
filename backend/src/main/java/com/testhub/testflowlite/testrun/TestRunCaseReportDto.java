package com.testhub.testflowlite.testrun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRunCaseReportDto {
    private Long caseId;
    private String code;
    private String title;
    private String assignedToName;
    private ResultStatus resultStatus;
    private String executedBy;
    private LocalDateTime executedAt;
    private String comment;
    private String defectRef;
}
