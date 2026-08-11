package com.testhub.testflowlite.testrun;

import com.testhub.testflowlite.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_run_cases")
@Getter
@Setter
@NoArgsConstructor
public class TestRunCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private TestRun run;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    // Snapshot fields (Rule 11 - point in time immutability)
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String precondition;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String steps;

    @Column(name = "expected_result", columnDefinition = "TEXT", nullable = false)
    private String expectedResult;

    @Column(name = "test_data", columnDefinition = "TEXT")
    private String testData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, columnDefinition = "VARCHAR(20)")
    private ResultStatus resultStatus = ResultStatus.UNTESTED;

    @Column(name = "executed_by", length = 100)
    private String executedBy;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "defect_ref")
    private String defectRef;

    @Column(name = "is_reviewed", nullable = false)
    private Boolean isReviewed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;
}
