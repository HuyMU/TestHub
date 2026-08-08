package com.testhub.testflowlite.section;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByProjectIdOrderBySortOrderAscIdAsc(Long projectId);

    int countByParentSectionId(Long parentSectionId);

    @Query(value = "SELECT COUNT(*) FROM test_cases WHERE section_id = :sectionId", nativeQuery = true)
    int countTestCasesBySectionId(@Param("sectionId") Long sectionId);

    @Query("SELECT s.parentSection.id, COUNT(s) FROM Section s WHERE s.project.id = :projectId AND s.parentSection IS NOT NULL GROUP BY s.parentSection.id")
    List<Object[]> countChildrenGroupedByParent(@Param("projectId") Long projectId);

    @Query(value = "SELECT section_id, COUNT(*) FROM test_cases WHERE section_id IN (SELECT id FROM sections WHERE project_id = :projectId) GROUP BY section_id", nativeQuery = true)
    List<Object[]> countTestCasesGroupedBySection(@Param("projectId") Long projectId);
}
