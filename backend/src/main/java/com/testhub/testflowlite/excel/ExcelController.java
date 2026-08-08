package com.testhub.testflowlite.excel;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/cases")
@RequiredArgsConstructor
@Tag(name = "Excel Import / Export", description = "Endpoints for 2-step Excel import validation/confirmation and export")
@SecurityRequirement(name = "bearerAuth")
public class ExcelController {

    private final ExcelService excelService;

    @PostMapping("/import/validate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Validate uploaded Excel file and return row-by-row errors with importSessionId")
    public ApiResponse<ExcelImportValidateResponse> validateImport(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        ExcelImportValidateResponse response = excelService.validateImport(projectId, file, userDetails.getUsername());
        return ApiResponse.success(response);
    }

    @PostMapping("/import/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirm Excel import by importSessionId and commit test cases to DB in Draft status")
    public ApiResponse<ExcelImportConfirmResponse> confirmImport(
            @PathVariable Long projectId,
            @Valid @RequestBody ExcelImportConfirmRequest request,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        ExcelImportConfirmResponse response = excelService.confirmImport(projectId, request, userDetails.getUsername());
        return ApiResponse.success(response);
    }

    @GetMapping("/import/template")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download formatted blank Excel import template (.xlsx)")
    public ResponseEntity<byte[]> downloadTemplate(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        byte[] fileBytes = excelService.generateTemplate(projectId, userDetails.getUsername());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"TestHub_Import_Template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(fileBytes);
    }

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Export project test cases to Excel (.xlsx) with sheet-per-section layout")
    public ResponseEntity<byte[]> exportCases(
            @PathVariable Long projectId,
            @RequestParam(required = false) List<Long> sectionIds,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        byte[] fileBytes = excelService.exportCases(projectId, sectionIds, userDetails.getUsername());
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "TestHub_Export_Project_" + projectId + "_" + dateStr + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(fileBytes);
    }
}
