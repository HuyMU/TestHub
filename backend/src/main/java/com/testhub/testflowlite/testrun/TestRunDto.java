package com.testhub.testflowlite.testrun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRunDto {
    private Long id;
    private Long projectId;
    private Long milestoneId;
    private String name;
    private String status;
    private List<Long> caseIds;
}
