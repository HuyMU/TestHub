package com.testhub.testflowlite.section;

import com.testhub.testflowlite.audit.AuditLogService;
import com.testhub.testflowlite.common.ConflictException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.project.Project;
import com.testhub.testflowlite.project.ProjectMemberRepository;
import com.testhub.testflowlite.project.ProjectRepository;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<SectionDto> getSectionTree(Long projectId, String currentUsername) {
        verifyProjectAccess(projectId, currentUsername);

        List<Section> flat = sectionRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId);
        Map<Long, SectionDto> dtoMap = new HashMap<>();
        List<SectionDto> rootNodes = new ArrayList<>();

        for (Section section : flat) {
            SectionDto dto = mapToDto(section);
            dtoMap.put(section.getId(), dto);
        }

        for (Section section : flat) {
            SectionDto dto = dtoMap.get(section.getId());
            Long parentId = section.getParentSection() != null ? section.getParentSection().getId() : null;

            if (parentId != null && dtoMap.containsKey(parentId)) {
                dtoMap.get(parentId).getChildren().add(dto);
            } else {
                rootNodes.add(dto);
            }
        }

        return rootNodes;
    }

    @Transactional
    public SectionDto createSection(Long projectId, CreateSectionRequest request, String currentUsername) {
        User currentUser = verifyProjectAccess(projectId, currentUsername);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        Section parentSection = null;
        if (request.getParentSectionId() != null) {
            parentSection = sectionRepository.findById(request.getParentSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent section not found with id: " + request.getParentSectionId()));
            if (!parentSection.getProject().getId().equals(projectId)) {
                throw new IllegalArgumentException("Parent section does not belong to project id: " + projectId);
            }
        }

        Section section = new Section();
        section.setProject(project);
        section.setParentSection(parentSection);
        section.setName(request.getName());
        section.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        Section saved = sectionRepository.save(section);
        auditLogService.logAction(currentUser.getId(), "CREATE_SECTION", "SECTION", saved.getId(), "Created section: " + saved.getName());

        return mapToDto(saved);
    }

    @Transactional
    public SectionDto updateSection(Long sectionId, UpdateSectionRequest request, String currentUsername) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + sectionId));

        Long projectId = section.getProject().getId();
        User currentUser = verifyProjectAccess(projectId, currentUsername);

        Section newParentSection = null;
        if (request.getParentSectionId() != null) {
            if (request.getParentSectionId().equals(sectionId)) {
                throw new IllegalArgumentException("Section cannot be its own parent");
            }
            // Check circular reference in parent chain
            checkCircularReference(sectionId, request.getParentSectionId());

            newParentSection = sectionRepository.findById(request.getParentSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent section not found with id: " + request.getParentSectionId()));
            if (!newParentSection.getProject().getId().equals(projectId)) {
                throw new IllegalArgumentException("Parent section does not belong to project id: " + projectId);
            }
        }

        section.setName(request.getName());
        section.setParentSection(newParentSection);
        if (request.getSortOrder() != null) {
            section.setSortOrder(request.getSortOrder());
        }

        Section updated = sectionRepository.save(section);
        auditLogService.logAction(currentUser.getId(), "UPDATE_SECTION", "SECTION", updated.getId(), "Updated section: " + updated.getName());

        return mapToDto(updated);
    }

    @Transactional
    public void reorderSections(Long projectId, ReorderSectionsRequest request, String currentUsername) {
        User currentUser = verifyProjectAccess(projectId, currentUsername);

        for (ReorderSectionItem item : request.getItems()) {
            Section section = sectionRepository.findById(item.getSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + item.getSectionId()));

            if (!section.getProject().getId().equals(projectId)) {
                throw new IllegalArgumentException("Section " + item.getSectionId() + " does not belong to project " + projectId);
            }

            if (item.getParentSectionId() != null) {
                if (item.getParentSectionId().equals(section.getId())) {
                    throw new IllegalArgumentException("Section cannot be its own parent");
                }
                checkCircularReference(section.getId(), item.getParentSectionId());
                Section parent = sectionRepository.findById(item.getParentSectionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent section not found with id: " + item.getParentSectionId()));
                section.setParentSection(parent);
            } else {
                section.setParentSection(null);
            }

            section.setSortOrder(item.getSortOrder());
            sectionRepository.save(section);
        }

        auditLogService.logAction(currentUser.getId(), "REORDER_SECTIONS", "PROJECT", projectId, "Reordered sections for project: " + projectId);
    }

    @Transactional
    public void deleteSection(Long sectionId, String currentUsername) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + sectionId));

        User currentUser = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        int directChildCount = sectionRepository.countByParentSectionId(sectionId);
        if (directChildCount > 0) {
            throw new ConflictException("Cannot delete section '" + section.getName() + "' because it contains " + directChildCount + " child subsection(s). Cascade deletion is forbidden.");
        }

        int directTestCaseCount = sectionRepository.countTestCasesBySectionId(sectionId);
        if (directTestCaseCount > 0) {
            throw new ConflictException("Cannot delete section '" + section.getName() + "' because it contains " + directTestCaseCount + " test case(s). Cascade deletion is forbidden.");
        }

        sectionRepository.delete(section);
        auditLogService.logAction(currentUser.getId(), "DELETE_SECTION", "SECTION", sectionId, "Deleted empty section: " + section.getName());
    }

    private User verifyProjectAccess(Long projectId, String currentUsername) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        User user = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        if (user.getRole() != Role.LEADER && !projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new AccessDeniedException("You do not have access to this project");
        }

        return user;
    }

    private void checkCircularReference(Long targetSectionId, Long candidateParentId) {
        Long currentId = candidateParentId;
        Set<Long> visited = new HashSet<>();

        while (currentId != null) {
            if (currentId.equals(targetSectionId)) {
                throw new IllegalArgumentException("Circular section reference detected: Section cannot be set as a child of its own descendant");
            }
            if (!visited.add(currentId)) {
                break;
            }
            Optional<Section> parentOpt = sectionRepository.findById(currentId);
            currentId = parentOpt.flatMap(s -> Optional.ofNullable(s.getParentSection())).map(Section::getId).orElse(null);
        }
    }

    private SectionDto mapToDto(Section section) {
        Long parentId = section.getParentSection() != null ? section.getParentSection().getId() : null;
        int subCount = sectionRepository.countByParentSectionId(section.getId());
        int caseCount = sectionRepository.countTestCasesBySectionId(section.getId());

        return new SectionDto(
                section.getId(),
                section.getProject().getId(),
                parentId,
                section.getName(),
                section.getSortOrder(),
                section.getCreatedAt(),
                caseCount,
                subCount,
                new ArrayList<>()
        );
    }
}
