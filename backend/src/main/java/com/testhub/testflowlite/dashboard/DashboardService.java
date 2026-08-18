package com.testhub.testflowlite.dashboard;

import com.testhub.testflowlite.milestone.Milestone;
import com.testhub.testflowlite.milestone.MilestoneRepository;
import com.testhub.testflowlite.project.ProjectAccessGuard;
import com.testhub.testflowlite.testcase.TestCaseRepository;
import com.testhub.testflowlite.testcase.TestCaseStatus;
import com.testhub.testflowlite.testrun.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectAccessGuard projectAccessGuard;
    private final TestCaseRepository testCaseRepository;
    private final TestRunRepository testRunRepository;
    private final TestRunCaseRepository testRunCaseRepository;
    private final MilestoneRepository milestoneRepository;

    @Transactional(readOnly = true)
    public DashboardDto getDashboard(Long projectId, String currentUsername) {
        projectAccessGuard.verifyProjectAccess(projectId, currentUsername);

        long totalCases = testCaseRepository.countBySectionProjectId(projectId);
        long readyCases = testCaseRepository.countBySectionProjectIdAndStatus(projectId, TestCaseStatus.READY);
        long reviewQueueCount = testCaseRepository.countBySectionProjectIdAndStatus(projectId, TestCaseStatus.REVIEW);

        List<TestRunCase> runCases = testRunCaseRepository.findByRunProjectId(projectId);
        long passedCount = runCases.stream().filter(rc -> rc.getResultStatus() == ResultStatus.PASSED).count();
        long failedCount = runCases.stream().filter(rc -> rc.getResultStatus() == ResultStatus.FAILED).count();
        long blockedCount = runCases.stream().filter(rc -> rc.getResultStatus() == ResultStatus.BLOCKED).count();
        long retestCount = runCases.stream().filter(rc -> rc.getResultStatus() == ResultStatus.RETEST).count();
        long untestedCount = runCases.stream().filter(rc -> rc.getResultStatus() == ResultStatus.UNTESTED).count();

        List<Milestone> milestones = milestoneRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<MilestoneProgressDto> milestoneProgress = milestones.stream().map(m -> {
            List<TestRun> milestoneRuns = testRunRepository.findByMilestoneId(m.getId());
            List<TestRunCase> mCases = testRunCaseRepository.findByRunMilestoneId(m.getId());
            long totalMCases = mCases.size();
            long completedMCases = mCases.stream().filter(rc -> rc.getResultStatus() != ResultStatus.UNTESTED).count();
            double pct = totalMCases > 0 ? (completedMCases * 100.0) / totalMCases : 0.0;
            return new MilestoneProgressDto(
                    m.getId(),
                    m.getName(),
                    m.getDueDate() != null ? m.getDueDate().toString() : null,
                    m.getStatus().name(),
                    milestoneRuns.size(),
                    totalMCases,
                    completedMCases,
                    Math.round(pct * 100.0) / 100.0
            );
        }).collect(Collectors.toList());

        return new DashboardDto(
                totalCases,
                readyCases,
                reviewQueueCount,
                passedCount,
                failedCount,
                blockedCount,
                retestCount,
                untestedCount,
                milestoneProgress
        );
    }
}
