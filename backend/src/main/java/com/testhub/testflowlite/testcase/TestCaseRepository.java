package com.testhub.testflowlite.testcase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long>, JpaSpecificationExecutor<TestCase> {

    List<TestCase> findByStatusOrderByCreatedAtAsc(TestCaseStatus status);

    boolean existsBySectionId(Long sectionId);
}
