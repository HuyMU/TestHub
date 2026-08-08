package com.testhub.testflowlite.milestone;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMilestoneRequest {
    private String name;
    private LocalDate dueDate;
    private MilestoneStatus status;
}
