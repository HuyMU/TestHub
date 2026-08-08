package com.testhub.testflowlite.milestone;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
    List<Milestone> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    boolean existsByProjectIdAndNameIgnoreCase(Long projectId, String name);
}
