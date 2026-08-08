package com.testhub.testflowlite.excel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExcelImportSessionRepository extends JpaRepository<ExcelImportSession, Long> {

    Optional<ExcelImportSession> findByImportSessionId(String importSessionId);

    void deleteByImportSessionId(String importSessionId);
}
