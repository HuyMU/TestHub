package com.testhub.testflowlite.attachment;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachment Management", description = "File uploads & secure authenticated download")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/upload")
    @Operation(summary = "Upload File Attachment")
    public ApiResponse<AttachmentDto> uploadFile(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestPart MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        AttachmentDto dto = attachmentService.uploadFile(entityType, entityId, file, userDetails.getUsername());
        return ApiResponse.success("File uploaded successfully", dto);
    }

    @GetMapping("/{attachmentId}/file")
    @Operation(summary = "Download / View Attachment File (Project Member Protected)")
    public ResponseEntity<Resource> getFile(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Resource resource = attachmentService.getFileResource(attachmentId, userDetails.getUsername());
        Attachment attachment = attachmentService.getAttachment(attachmentId);

        String contentType = null;
        try {
            contentType = Files.probeContentType(resource.getFile().toPath());
        } catch (IOException ignored) {
        }
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
