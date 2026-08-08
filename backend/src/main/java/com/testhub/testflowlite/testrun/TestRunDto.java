package com.testhub.testflowlite.testrun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRunDto {
    private Long id;
    private Long projectId;
    private Long milestoneId;
    private String milestoneName;
    private String name;
    private RunStatus status;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;

    private int totalCases;
    private int passedCases;
    private int failedCases;
    private int blockedCases;
    private int untestedCases;

    private List<TestRunCaseDto> cases;
}
