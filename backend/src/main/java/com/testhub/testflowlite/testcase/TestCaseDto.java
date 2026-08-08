package com.testhub.testflowlite.testcase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseDto {
    private Long id;
    private String code;
    private Long projectId;
    private Long sectionId;
    private String sectionName;
    private String title;
    private String precondition;
    private String steps;
    private String expectedResult;
    private String testData;
    private Priority priority;
    private TestCaseType type;
    private AutomationStatus automationStatus;
    private TestCaseStatus status;
    private String reviewComment;
    private Long createdById;
    private String createdByFullName;
    private Long reviewedById;
    private String reviewedByFullName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
