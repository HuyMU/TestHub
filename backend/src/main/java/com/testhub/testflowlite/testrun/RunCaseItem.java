package com.testhub.testflowlite.testrun;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunCaseItem {

    @NotNull(message = "Case ID is required")
    private Long caseId;

    private Long assignedToId;
}
