package com.testhub.testflowlite.excel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportRowDto {
    private int rowNumber;
    private String sheetName;
    private String subsectionPath;
    private String title;
    private String precondition;
    private String steps;
    private String expectedResult;
    private String testData;
    private String priority;
    private String type;
    private String automationStatus;
    private SectionPathMode sectionPathMode = SectionPathMode.FULL_PATH;
    private List<String> errors = new ArrayList<>();
}
