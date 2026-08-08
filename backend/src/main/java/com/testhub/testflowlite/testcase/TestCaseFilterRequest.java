package com.testhub.testflowlite.testcase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseFilterRequest {
    private Long sectionId;
    private Priority priority;
    private TestCaseType type;
    private TestCaseStatus status;
    private AutomationStatus automationStatus;
    private String keyword;
}
