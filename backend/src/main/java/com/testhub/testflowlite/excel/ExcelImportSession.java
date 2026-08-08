package com.testhub.testflowlite.excel;

import com.testhub.testflowlite.project.Project;
import com.testhub.testflowlite.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "excel_import_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ExcelImportSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_session_id", nullable = false, unique = true, length = 50)
    private String importSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "parsed_payload_json", columnDefinition = "LONGTEXT")
    private String parsedPayloadJson;

    @Column(name = "error_lines_json", columnDefinition = "TEXT")
    private String errorLinesJson;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
