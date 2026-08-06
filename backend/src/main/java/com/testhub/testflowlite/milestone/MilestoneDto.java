package com.testhub.testflowlite.milestone;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneDto {
    private Long id;
    private Long projectId;
    private String name;
    private LocalDate dueDate;
    private String status;
}
