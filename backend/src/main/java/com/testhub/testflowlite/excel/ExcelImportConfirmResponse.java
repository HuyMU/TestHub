package com.testhub.testflowlite.excel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportConfirmResponse {
    private int createdCasesCount;
    private int createdSectionsCount;
    private Map<String, Integer> casesPerSheet;
}
