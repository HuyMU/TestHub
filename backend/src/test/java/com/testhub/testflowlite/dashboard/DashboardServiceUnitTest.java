package com.testhub.testflowlite.dashboard;

import com.testhub.testflowlite.common.ForbiddenException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.milestone.MilestoneRepository;
import com.testhub.testflowlite.project.ProjectAccessGuard;
import com.testhub.testflowlite.project.ProjectMemberRepository;
import com.testhub.testflowlite.project.ProjectRepository;
import com.testhub.testflowlite.testcase.TestCaseRepository;
import com.testhub.testflowlite.testrun.TestRunCaseRepository;
import com.testhub.testflowlite.testrun.TestRunRepository;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceUnitTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private TestRunRepository testRunRepository;

    @Mock
    private TestRunCaseRepository testRunCaseRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    private ProjectAccessGuard projectAccessGuard;
    private DashboardService dashboardService;

    private User leaderUser;
    private User testerUser;

    @BeforeEach
    void setUp() {
        projectAccessGuard = new ProjectAccessGuard(projectRepository, projectMemberRepository, userRepository);
        dashboardService = new DashboardService(
                projectAccessGuard,
                testCaseRepository,
                testRunRepository,
                testRunCaseRepository,
                milestoneRepository
        );

        leaderUser = new User();
        leaderUser.setId(1L);
        leaderUser.setUsername("leader");
        leaderUser.setRole(Role.LEADER);

        testerUser = new User();
        testerUser.setId(2L);
        testerUser.setUsername("tester");
        testerUser.setRole(Role.TESTER);

        lenient().when(projectRepository.existsById(10L)).thenReturn(true);
        lenient().when(userRepository.findByUsernameOrEmail("leader", "leader")).thenReturn(Optional.of(leaderUser));
        lenient().when(userRepository.findByUsernameOrEmail("tester", "tester")).thenReturn(Optional.of(testerUser));
        lenient().when(testRunCaseRepository.findByRunProjectId(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(milestoneRepository.findByProjectIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());
    }

    @Test
    void testGetDashboard_ExistingProject_Leader_ReturnsDashboardDto() {
        DashboardDto dto = dashboardService.getDashboard(10L, "leader");

        assertNotNull(dto);
        assertEquals(0, dto.getTotalCases());
        assertEquals(0, dto.getReadyCases());
    }

    @Test
    void testGetDashboard_ExistingProject_TesterMember_ReturnsDashboardDto() {
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 2L)).thenReturn(true);

        DashboardDto dto = dashboardService.getDashboard(10L, "tester");

        assertNotNull(dto);
    }

    @Test
    void testGetDashboard_ExistingProject_TesterNonMember_ThrowsForbiddenException() {
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 2L)).thenReturn(false);

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> dashboardService.getDashboard(10L, "tester")
        );

        assertEquals("You do not have access to this project", ex.getMessage());
    }

    @Test
    void testGetDashboard_NonExistentProject_Leader_ThrowsResourceNotFoundException() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> dashboardService.getDashboard(999L, "leader")
        );

        assertEquals("Project not found: 999", ex.getMessage());
        verify(testCaseRepository, never()).countBySectionProjectId(anyLong());
    }

    @Test
    void testGetDashboard_NonExistentProject_Tester_ThrowsResourceNotFoundException() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> dashboardService.getDashboard(999L, "tester")
        );

        assertEquals("Project not found: 999", ex.getMessage());
        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());
    }
}
