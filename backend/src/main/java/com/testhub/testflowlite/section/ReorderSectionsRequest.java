package com.testhub.testflowlite.section;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderSectionsRequest {

    @NotEmpty(message = "Reorder items list cannot be empty")
    @Valid
    private List<ReorderSectionItem> items;
}
