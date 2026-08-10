package com.testhub.testflowlite.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {
    private long totalCases;
    private long readyCases;
    private long reviewQueueCount;
    private long passedCount;
    private long failedCount;
    private long blockedCount;
    private long retestCount;
    private long untestedCount;
    private List<MilestoneProgressDto> milestoneProgress;
}
