package com.testhub.testflowlite.testrun;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddCasesToRunRequest {

    private Boolean includeNonReady = false;

    @NotEmpty(message = "Case list cannot be empty")
    private List<RunCaseItem> cases;

    public AddCasesToRunRequest(List<RunCaseItem> cases) {
        this.includeNonReady = false;
        this.cases = cases;
    }
}
