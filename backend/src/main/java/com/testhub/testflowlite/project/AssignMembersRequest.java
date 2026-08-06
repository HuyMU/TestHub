package com.testhub.testflowlite.project;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignMembersRequest {

    @NotEmpty(message = "User IDs list cannot be empty")
    private List<Long> userIds;
}
