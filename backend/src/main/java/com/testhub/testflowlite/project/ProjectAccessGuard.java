package com.testhub.testflowlite.project;

import com.testhub.testflowlite.common.ForbiddenException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAccessGuard {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    /**
     * Resolves the current user by username/email and throws ForbiddenException (403) if they are
     * not the Leader and not a member of the given project. Throws ResourceNotFoundException (404)
     * if the project itself does not exist. Returns the resolved User since most call sites need it
     * immediately after the check.
     */
    public User verifyProjectAccess(Long projectId, String currentUsername) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }
        User user = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));
        if (!hasProjectAccess(projectId, user.getId(), user.getRole())) {
            throw new ForbiddenException("You do not have access to this project");
        }
        return user;
    }

    /**
     * Boolean check for composition — use when a caller needs to OR this with other conditions
     * (e.g. AttachmentService's project-creator bypass) or needs a custom exception/message.
     * Deliberately does NOT check project existence — callers using this method already have the
     * project entity resolved via a JPA relationship (e.g. runCase.getRun().getProject()), so an
     * extra existence query here would be pure overhead on hot paths (execution/attachment checks).
     */
    public boolean hasProjectAccess(Long projectId, Long userId, Role role) {
        return role == Role.LEADER || projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }
}
