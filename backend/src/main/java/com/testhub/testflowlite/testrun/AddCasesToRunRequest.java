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

    @NotEmpty(message = "Case list cannot be empty")
    private List<RunCaseItem> cases;
}
