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
}
