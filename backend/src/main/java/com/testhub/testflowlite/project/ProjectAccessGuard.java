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

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    /**
     * Resolves the current user by username/email and throws ForbiddenException (403) if they are
     * not the Leader and not a member of the given project. Returns the resolved User since most
     * call sites need it immediately after the check.
     */
    public User verifyProjectAccess(Long projectId, String currentUsername) {
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
     */
    public boolean hasProjectAccess(Long projectId, Long userId, Role role) {
        return role == Role.LEADER || projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }
}
