package com.testhub.testflowlite.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {
    private Long projectId;
    private int totalCases;
    private int readyCases;
    private int reviewQueueCount;
    private int passedCount;
    private int failedCount;
    private int blockedCount;
}
