package com.testhub.testflowlite.testrun;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRunCaseRepository extends JpaRepository<TestRunCase, Long> {
    List<TestRunCase> findByRunIdOrderByIdAsc(Long runId);
    boolean existsByRunIdAndCaseId(Long runId, Long caseId);
}
