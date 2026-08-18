package com.testhub.testflowlite.attachment;

import com.testhub.testflowlite.common.BadRequestException;
import com.testhub.testflowlite.common.ForbiddenException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.execution.ExecutionHistory;
import com.testhub.testflowlite.execution.ExecutionHistoryRepository;
import com.testhub.testflowlite.project.Project;
import com.testhub.testflowlite.project.ProjectAccessGuard;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final ExecutionHistoryRepository executionHistoryRepository;
    private final ProjectAccessGuard projectAccessGuard;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Transactional
    public AttachmentDto uploadFile(String entityType, Long entityId, MultipartFile file, String currentUsername) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds 10MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            throw new BadRequestException("Invalid file format. Only images and PDF files are allowed.");
        }

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        String sanitizeEntityType = entityType.replaceAll("[^a-zA-Z0-9_-]", "_").toUpperCase();

        // Verify authorization for target entity
        verifyEntityAccess(sanitizeEntityType, entityId, user);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "file";
        }
        String cleanOriginalName = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

        String filename = UUID.randomUUID() + "-" + cleanOriginalName;

        Path dirPath = Paths.get("uploads", sanitizeEntityType, String.valueOf(entityId));
        File dir = dirPath.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File destFile = dirPath.resolve(filename).toFile();
        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file locally: " + e.getMessage(), e);
        }

        String storedPath = destFile.getPath();

        Attachment attachment = new Attachment();
        attachment.setEntityType(sanitizeEntityType);
        attachment.setEntityId(entityId);
        attachment.setFilePath(storedPath);
        attachment.setUploadedBy(user);
        attachment = attachmentRepository.save(attachment);

        return mapToDto(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentDto> listByEntity(String entityType, Long entityId) {
        String sanitizeEntityType = entityType.toUpperCase();
        return attachmentRepository.findByEntityTypeAndEntityId(sanitizeEntityType, entityId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Resource getFileResource(Long attachmentId, String currentUsername) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        verifyEntityAccess(attachment.getEntityType(), attachment.getEntityId(), user);

        File file = new File(attachment.getFilePath());
        if (!file.exists() || !file.canRead()) {
            throw new ResourceNotFoundException("Attachment file not found on disk");
        }

        return new FileSystemResource(file);
    }

    @Transactional(readOnly = true)
    public Attachment getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));
    }

    private void verifyEntityAccess(String entityType, Long entityId, User user) {
        if ("EXECUTION_HISTORY".equalsIgnoreCase(entityType)) {
            ExecutionHistory history = executionHistoryRepository.findById(entityId)
                    .orElseThrow(() -> new ResourceNotFoundException("ExecutionHistory not found: " + entityId));

            Project project = history.getRunCase().getRun().getProject();
            boolean isCreator = project.getCreatedBy() != null && project.getCreatedBy().getId().equals(user.getId());
            boolean hasAccess = isCreator || projectAccessGuard.hasProjectAccess(project.getId(), user.getId(), user.getRole());

            if (!hasAccess) {
                throw new ForbiddenException("You do not have access to attachments for this project");
            }
        }
    }

    private AttachmentDto mapToDto(Attachment a) {
        String fileName = new File(a.getFilePath()).getName();
        String downloadUrl = "/api/attachments/" + a.getId() + "/file";
        return new AttachmentDto(
                a.getId(),
                a.getEntityType(),
                a.getEntityId(),
                fileName,
                downloadUrl,
                a.getUploadedBy() != null ? a.getUploadedBy().getId() : null,
                a.getUploadedBy() != null ? a.getUploadedBy().getFullName() : null,
                a.getUploadedAt()
        );
    }
}
