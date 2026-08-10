package com.testhub.testflowlite.testcase;

import com.testhub.testflowlite.common.BaseEntity;
import com.testhub.testflowlite.section.Section;
import com.testhub.testflowlite.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
public class TestCase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String precondition;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String steps;

    @Column(name = "expected_result", columnDefinition = "TEXT", nullable = false)
    private String expectedResult;

    @Column(name = "test_data", columnDefinition = "TEXT")
    private String testData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TestCaseType type = TestCaseType.FUNCTIONAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "automation_status", nullable = false, length = 20)
    private AutomationStatus automationStatus = AutomationStatus.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestCaseStatus status = TestCaseStatus.DRAFT;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}
