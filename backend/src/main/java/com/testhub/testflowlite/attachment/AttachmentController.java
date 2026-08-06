package com.testhub.testflowlite.attachment;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachment Management", description = "Local filesystem file uploads (/uploads)")
public class AttachmentController {

    @PostMapping("/upload")
    @Operation(summary = "Upload File Attachment")
    public ApiResponse<String> uploadFile(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestPart MultipartFile file) {
        // TODO: Implement file saving to local filesystem
        return ApiResponse.success("File uploaded successfully", "/uploads/mock-file.jpg");
    }
}
