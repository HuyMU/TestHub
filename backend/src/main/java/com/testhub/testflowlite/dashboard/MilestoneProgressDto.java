package com.testhub.testflowlite.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneProgressDto {
    private Long milestoneId;
    private String milestoneName;
    private String dueDate;
    private String status;
    private long totalRuns;
    private long totalCases;
    private long completedCases;
    private double progressPercentage;
}
