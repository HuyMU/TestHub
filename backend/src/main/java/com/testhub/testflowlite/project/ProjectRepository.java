package com.testhub.testflowlite.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOrderByIdDesc();

    @Query("SELECT pm.project FROM ProjectMember pm WHERE pm.user.id = :userId ORDER BY pm.project.id DESC")
    List<Project> findProjectsByUserId(@Param("userId") Long userId);
}
