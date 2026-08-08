package com.testhub.testflowlite.testcase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTestCaseRequest {

    @NotNull(message = "Section ID is required")
    private Long sectionId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String precondition;

    @NotBlank(message = "Steps are required")
    private String steps;

    @NotBlank(message = "Expected result is required")
    private String expectedResult;

    private String testData;

    @NotNull(message = "Priority is required")
    private Priority priority;

    @NotNull(message = "Test type is required")
    private TestCaseType type;

    @NotNull(message = "Automation status is required")
    private AutomationStatus automationStatus;
}
