package com.testhub.testflowlite.excel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportValidateResponse {
    private String importSessionId;
    private int totalRows;
    private int errorRowsCount;
    private List<ExcelImportRowDto> rows;
}
