package com.testhub.testflowlite.section;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSectionRequest {

    @NotBlank(message = "Section name is required")
    @Size(max = 255, message = "Section name must not exceed 255 characters")
    private String name;

    private Long parentSectionId;

    private Integer sortOrder;
}
