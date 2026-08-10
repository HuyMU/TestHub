package com.testhub.testflowlite.testcase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long>, JpaSpecificationExecutor<TestCase> {

    List<TestCase> findByStatusOrderBySubmittedAtAsc(TestCaseStatus status);

    Optional<TestCase> findByCode(String code);

    boolean existsBySectionId(Long sectionId);
}
