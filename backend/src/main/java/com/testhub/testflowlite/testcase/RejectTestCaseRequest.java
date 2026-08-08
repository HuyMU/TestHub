package com.testhub.testflowlite.testcase;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectTestCaseRequest {

    @NotBlank(message = "Review comment is required when rejecting a test case")
    private String reviewComment;
}
