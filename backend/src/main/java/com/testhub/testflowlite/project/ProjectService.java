package com.testhub.testflowlite.project;

import com.testhub.testflowlite.audit.AuditLogService;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserDto;
import com.testhub.testflowlite.user.UserRepository;
import com.testhub.testflowlite.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;

    @Transactional
    public ProjectDto createProject(CreateProjectRequest request, String currentUsername) {
        User creator = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStatus("Active");
        project.setCreatedBy(creator);

        Project saved = projectRepository.save(project);
        auditLogService.logAction(creator.getId(), "CREATE_PROJECT", "PROJECT", saved.getId(), "Created project: " + saved.getName());

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getAllProjects(String currentUsername) {
        User currentUser = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        List<Project> projects;
        if (currentUser.getRole() == Role.LEADER) {
            projects = projectRepository.findAllByOrderByIdDesc();
        } else {
            projects = projectRepository.findProjectsByUserId(currentUser.getId());
        }

        return projects.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id, String currentUsername) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        User currentUser = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        if (currentUser.getRole() != Role.LEADER && !projectMemberRepository.existsByProjectIdAndUserId(id, currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this project");
        }

        return mapToDto(project);
    }

    @Transactional
    public ProjectDto updateProject(Long id, UpdateProjectRequest request, String currentUsername) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        User currentUser = userRepository.findByUsernameOrEmail(currentUsername, currentUsername).orElse(null);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStatus(request.getStatus());

        Project updated = projectRepository.save(project);
        auditLogService.logAction(currentUserId, "UPDATE_PROJECT", "PROJECT", updated.getId(),
                "Updated project status=" + updated.getStatus() + ", name=" + updated.getName());

        return mapToDto(updated);
    }

    @Transactional
    public void assignMembers(Long projectId, AssignMembersRequest request, String currentUsername) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        User currentUser = userRepository.findByUsernameOrEmail(currentUsername, currentUsername).orElse(null);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;

        for (Long userId : request.getUserIds()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            if (user.getRole() != Role.TESTER) {
                throw new IllegalArgumentException("Cannot assign non-Tester user (id: " + userId + ") to project");
            }
            if (Boolean.FALSE.equals(user.getIsActive())) {
                throw new IllegalArgumentException("Cannot assign disabled user (id: " + userId + ") to project");
            }

            if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
                ProjectMember pm = new ProjectMember();
                pm.setProject(project);
                pm.setUser(user);
                projectMemberRepository.save(pm);
            }
        }

        auditLogService.logAction(currentUserId, "ASSIGN_PROJECT_MEMBERS", "PROJECT", projectId,
                "Assigned members: " + request.getUserIds());
    }

    @Transactional
    public void removeMember(Long projectId, Long userId, String currentUsername) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        User currentUser = userRepository.findByUsernameOrEmail(currentUsername, currentUsername).orElse(null);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;

        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
        auditLogService.logAction(currentUserId, "REMOVE_PROJECT_MEMBER", "PROJECT", projectId,
                "Removed user id: " + userId + " from project");
    }

    @Transactional(readOnly = true)
    public List<UserDto> getProjectMembers(Long projectId, String currentUsername) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        User currentUser = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        if (currentUser.getRole() != Role.LEADER && !projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this project");
        }

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        return members.stream()
                .map(pm -> userService.mapToDto(pm.getUser()))
                .collect(Collectors.toList());
    }

    public ProjectDto mapToDto(Project project) {
        UserDto creatorDto = project.getCreatedBy() != null ? userService.mapToDto(project.getCreatedBy()) : null;
        int memberCount = projectMemberRepository.countByProjectId(project.getId());
        return new ProjectDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                creatorDto,
                project.getCreatedAt(),
                memberCount
        );
    }
}
