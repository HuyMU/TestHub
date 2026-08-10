package com.testhub.testflowlite.execution;

import com.testhub.testflowlite.testrun.ResultStatus;
import com.testhub.testflowlite.testrun.TestRunCase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "execution_history")
@Getter
@Setter
@NoArgsConstructor
public class ExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_case_id", nullable = false)
    private TestRunCase runCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 20)
    private ResultStatus resultStatus;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "executed_by", length = 100)
    private String executedBy;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt = LocalDateTime.now();
}
