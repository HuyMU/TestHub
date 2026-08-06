package com.testhub.testflowlite.section;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderSectionItem {

    @NotNull(message = "Section ID is required")
    private Long sectionId;

    @NotNull(message = "Sort order is required")
    private Integer sortOrder;

    private Long parentSectionId;
}
