package com.testhub.testflowlite.section;

import com.testhub.testflowlite.common.BaseEntity;
import com.testhub.testflowlite.project.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
public class Section extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_section_id")
    private Section parentSection;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
