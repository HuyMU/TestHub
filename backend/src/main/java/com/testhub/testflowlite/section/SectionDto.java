package com.testhub.testflowlite.section;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionDto {
    private Long id;
    private Long projectId;
    private Long parentSectionId;
    private String name;
    private Integer sortOrder;
    private List<SectionDto> children;
}
