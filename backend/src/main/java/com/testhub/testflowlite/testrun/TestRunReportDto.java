package com.testhub.testflowlite.testrun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRunReportDto {
    private Long runId;
    private String runName;
    private String projectName;
    private String milestoneName;
    private RunStatus runStatus;
    private LocalDateTime closedAt;
    private long totalCases;
    private long passedCases;
    private long failedCases;
    private long blockedCases;
    private long retestCases;
    private long untestedCases;
    private double passRatePercentage;
    private double completionPercentage;
    private List<TestRunCaseReportDto> cases;
}
