package com.testhub.testflowlite.execution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionHistoryRepository extends JpaRepository<ExecutionHistory, Long> {
    List<ExecutionHistory> findByRunCaseIdOrderByExecutedAtDesc(Long runCaseId);
    List<ExecutionHistory> findByRunCaseIdOrderByExecutedAtDescIdDesc(Long runCaseId);
    List<ExecutionHistory> findByRunCaseRunIdAndRunCaseCaseIdOrderByExecutedAtDesc(Long runId, Long caseId);
    List<ExecutionHistory> findByRunCaseRunIdAndRunCaseCaseIdOrderByExecutedAtDescIdDesc(Long runId, Long caseId);
}
