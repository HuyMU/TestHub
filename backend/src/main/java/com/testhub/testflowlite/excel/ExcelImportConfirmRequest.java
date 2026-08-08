package com.testhub.testflowlite.excel;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportConfirmRequest {

    @NotBlank(message = "Import Session ID is required")
    private String importSessionId;
}
