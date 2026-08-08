package com.testhub.testflowlite.milestone;

import com.testhub.testflowlite.audit.AuditLogService;
import com.testhub.testflowlite.common.ConflictException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.project.Project;
import com.testhub.testflowlite.project.ProjectMemberRepository;
import com.testhub.testflowlite.project.ProjectRepository;
import com.testhub.testflowlite.testrun.TestRunRepository;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TestRunRepository testRunRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<MilestoneDto> getMilestones(Long projectId, String currentUsername) {
        verifyProjectAccess(projectId, currentUsername);
        List<Milestone> milestones = milestoneRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        return milestones.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public MilestoneDto createMilestone(Long projectId, CreateMilestoneRequest request, String currentUsername) {
        User currentUser = verifyLeaderAccess(projectId, currentUsername);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        if (milestoneRepository.existsByProjectIdAndNameIgnoreCase(projectId, request.getName().trim())) {
            throw new ConflictException("Milestone name already exists in project: " + request.getName());
        }

        Milestone milestone = new Milestone();
        milestone.setProject(project);
        milestone.setName(request.getName().trim());
        milestone.setDueDate(request.getDueDate());
        milestone.setStatus(MilestoneStatus.OPEN);
        milestone.setCreatedBy(currentUser);

        Milestone saved = milestoneRepository.save(milestone);
        auditLogService.logAction(currentUser.getId(), "CREATE_MILESTONE", "MILESTONE", saved.getId(), "Created milestone " + saved.getName());

        return mapToDto(saved);
    }

    @Transactional
    public MilestoneDto updateMilestone(Long projectId, Long milestoneId, UpdateMilestoneRequest request, String currentUsername) {
        User currentUser = verifyLeaderAccess(projectId, currentUsername);

        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found: " + milestoneId));

        if (!milestone.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Milestone " + milestoneId + " does not belong to project " + projectId);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(milestone.getName()) && milestoneRepository.existsByProjectIdAndNameIgnoreCase(projectId, newName)) {
                throw new ConflictException("Milestone name already exists in project: " + newName);
            }
            milestone.setName(newName);
        }

        if (request.getDueDate() != null) {
            milestone.setDueDate(request.getDueDate());
        }

        if (request.getStatus() != null) {
            milestone.setStatus(request.getStatus());
        }

        Milestone updated = milestoneRepository.save(milestone);
        auditLogService.logAction(currentUser.getId(), "UPDATE_MILESTONE", "MILESTONE", updated.getId(), "Updated milestone " + updated.getName());

        return mapToDto(updated);
    }

    @Transactional
    public void deleteMilestone(Long projectId, Long milestoneId, String currentUsername) {
        User currentUser = verifyLeaderAccess(projectId, currentUsername);

        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found: " + milestoneId));

        if (!milestone.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Milestone " + milestoneId + " does not belong to project " + projectId);
        }

        // Check if milestone has associated Test Runs
        if (testRunRepository.existsByMilestoneId(milestoneId)) {
            throw new ConflictException("Cannot delete milestone because it is referenced by existing Test Runs");
        }

        milestoneRepository.delete(milestone);
        auditLogService.logAction(currentUser.getId(), "DELETE_MILESTONE", "MILESTONE", milestoneId, "Deleted milestone " + milestone.getName());
    }

    private User verifyProjectAccess(Long projectId, String currentUsername) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }

        User user = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        if (user.getRole() != Role.LEADER && !projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new AccessDeniedException("You do not have access to this project");
        }

        return user;
    }

    private User verifyLeaderAccess(Long projectId, String currentUsername) {
        User user = verifyProjectAccess(projectId, currentUsername);
        if (user.getRole() != Role.LEADER) {
            throw new AccessDeniedException("Leader role required for milestone management");
        }
        return user;
    }

    private MilestoneDto mapToDto(Milestone m) {
        return new MilestoneDto(
                m.getId(),
                m.getProject().getId(),
                m.getName(),
                m.getDueDate(),
                m.getStatus(),
                m.getCreatedBy() != null ? m.getCreatedBy().getId() : null,
                m.getCreatedBy() != null ? m.getCreatedBy().getFullName() : null,
                m.getCreatedAt()
        );
    }
}
