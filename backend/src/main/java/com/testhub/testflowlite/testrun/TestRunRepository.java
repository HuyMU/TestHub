package com.testhub.testflowlite.testrun;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {
    List<TestRun> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    boolean existsByMilestoneId(Long milestoneId);
    List<TestRun> findByMilestoneId(Long milestoneId);
}
