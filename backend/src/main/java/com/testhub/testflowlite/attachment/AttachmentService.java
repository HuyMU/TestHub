package com.testhub.testflowlite.attachment;

import com.testhub.testflowlite.common.BadRequestException;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Transactional
    public String uploadFile(String entityType, Long entityId, MultipartFile file, String currentUsername) {
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

        User user = userRepository.findByUsername(currentUsername).orElse(null);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "file";
        }
        originalFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

        String filename = UUID.randomUUID() + "-" + originalFilename;
        String sanitizeEntityType = entityType.replaceAll("[^a-zA-Z0-9_-]", "_").toUpperCase();

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

        String relativeUrl = "/uploads/" + sanitizeEntityType + "/" + entityId + "/" + filename;

        Attachment attachment = new Attachment();
        attachment.setEntityType(sanitizeEntityType);
        attachment.setEntityId(entityId);
        attachment.setFilePath(relativeUrl);
        attachment.setUploadedBy(user);
        attachmentRepository.save(attachment);

        return relativeUrl;
    }
}
