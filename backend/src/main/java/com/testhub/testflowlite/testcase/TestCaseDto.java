package com.testhub.testflowlite.testcase;

import com.testhub.testflowlite.common.AutomationStatus;
import com.testhub.testflowlite.common.CaseStatus;
import com.testhub.testflowlite.common.Priority;
import com.testhub.testflowlite.common.TestType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseDto {
    private Long id;
    private String code;
    private Long sectionId;
    private String title;
    private String precondition;
    private String steps;
    private String expectedResult;
    private String testData;
    private Priority priority;
    private TestType type;
    private AutomationStatus automationStatus;
    private CaseStatus status;
    private String reviewComment;
}
