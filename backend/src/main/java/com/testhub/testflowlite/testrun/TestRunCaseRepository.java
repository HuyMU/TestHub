package com.testhub.testflowlite.testrun;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRunCaseRepository extends JpaRepository<TestRunCase, Long> {
    List<TestRunCase> findByRunIdOrderByIdAsc(Long runId);
    boolean existsByRunIdAndCaseId(Long runId, Long caseId);
    Optional<TestRunCase> findByRunIdAndCaseId(Long runId, Long caseId);
    List<TestRunCase> findByRunProjectId(Long projectId);
    List<TestRunCase> findByRunMilestoneId(Long milestoneId);
}
