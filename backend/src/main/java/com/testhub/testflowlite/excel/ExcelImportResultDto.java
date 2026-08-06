package com.testhub.testflowlite.excel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportResultDto {
    private String importToken;
    private int totalRows;
    private int validRows;
    private int errorRows;
    private List<String> rowErrors = new ArrayList<>();
}
