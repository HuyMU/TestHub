package com.testhub.testflowlite.testrun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRunCaseDto {
    private Long id;
    private Long runId;
    private Long caseId;
    private String code;
    private String title;
    private String precondition;
    private String steps;
    private String expectedResult;
    private String testData;
    private Long assignedToId;
    private String assignedToName;
    private ResultStatus resultStatus;
    private String executedBy;
    private LocalDateTime executedAt;
    private String comment;
    private String defectRef;
    private Boolean isReviewed;
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private Long latestExecutionHistoryId;
}
