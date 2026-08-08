package com.testhub.testflowlite.testrun;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestRunRequest {

    @NotBlank(message = "Test Run name is required")
    private String name;

    private Long milestoneId;

    private Boolean includeNonReady = false;

    private List<RunCaseItem> cases = new ArrayList<>();
}
