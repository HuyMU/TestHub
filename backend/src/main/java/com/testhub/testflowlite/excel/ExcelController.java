package com.testhub.testflowlite.excel;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Tag(name = "Excel Import / Export", description = "Apache POI Excel validation & confirmation")
public class ExcelController {

    @PostMapping(value = "/import/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Step 1: Validate Excel Template & Return Line Errors")
    public ApiResponse<ExcelImportResultDto> validateImport(@RequestPart("file") MultipartFile file) {
        // TODO: Implement POI validation logic
        return ApiResponse.success("Excel validation passed", new ExcelImportResultDto());
    }

    @PostMapping("/import/confirm")
    @Operation(summary = "Step 2: Confirm Import to Database (Draft Status)")
    public ApiResponse<String> confirmImport(@RequestBody String importToken) {
        // TODO: Implement confirmation persistence to DB
        return ApiResponse.success("Import confirmed. Cases created in Draft status.", "Success");
    }
}
