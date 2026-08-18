package com.testhub.testflowlite.project;

import com.testhub.testflowlite.common.ForbiddenException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectAccessGuardUnitTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectAccessGuard projectAccessGuard;

    private User leaderUser;
    private User testerUser;

    @BeforeEach
    void setUp() {
        leaderUser = new User();
        leaderUser.setId(1L);
        leaderUser.setUsername("leader");
        leaderUser.setRole(Role.LEADER);

        testerUser = new User();
        testerUser.setId(2L);
        testerUser.setUsername("tester1");
        testerUser.setRole(Role.TESTER);
    }

    @Test
    void testVerifyProjectAccess_LeaderAlwaysPasses() {
        when(userRepository.findByUsernameOrEmail("leader", "leader"))
                .thenReturn(Optional.of(leaderUser));

        User result = projectAccessGuard.verifyProjectAccess(100L, "leader");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("leader", result.getUsername());
        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void testVerifyProjectAccess_TesterMemberPasses() {
        when(userRepository.findByUsernameOrEmail("tester1", "tester1"))
                .thenReturn(Optional.of(testerUser));
        when(projectMemberRepository.existsByProjectIdAndUserId(100L, 2L))
                .thenReturn(true);

        User result = projectAccessGuard.verifyProjectAccess(100L, "tester1");

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("tester1", result.getUsername());
        verify(projectMemberRepository).existsByProjectIdAndUserId(100L, 2L);
    }

    @Test
    void testVerifyProjectAccess_TesterNonMemberThrowsForbidden() {
        when(userRepository.findByUsernameOrEmail("tester1", "tester1"))
                .thenReturn(Optional.of(testerUser));
        when(projectMemberRepository.existsByProjectIdAndUserId(100L, 2L))
                .thenReturn(false);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> projectAccessGuard.verifyProjectAccess(100L, "tester1")
        );

        assertEquals("You do not have access to this project", exception.getMessage());
    }

    @Test
    void testVerifyProjectAccess_UserNotFoundThrowsNotFound() {
        when(userRepository.findByUsernameOrEmail("unknown", "unknown"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> projectAccessGuard.verifyProjectAccess(100L, "unknown")
        );

        assertEquals("User not found: unknown", exception.getMessage());
    }

    @Test
    void testHasProjectAccess_LeaderReturnsTrueWithoutDbCheck() {
        assertTrue(projectAccessGuard.hasProjectAccess(100L, 1L, Role.LEADER));
        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void testHasProjectAccess_TesterMemberReturnsTrue() {
        when(projectMemberRepository.existsByProjectIdAndUserId(100L, 2L)).thenReturn(true);

        assertTrue(projectAccessGuard.hasProjectAccess(100L, 2L, Role.TESTER));
        verify(projectMemberRepository).existsByProjectIdAndUserId(100L, 2L);
    }

    @Test
    void testHasProjectAccess_TesterNonMemberReturnsFalse() {
        when(projectMemberRepository.existsByProjectIdAndUserId(100L, 2L)).thenReturn(false);

        assertFalse(projectAccessGuard.hasProjectAccess(100L, 2L, Role.TESTER));
        verify(projectMemberRepository).existsByProjectIdAndUserId(100L, 2L);
    }
}
