package com.testhub.testflowlite.excel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testhub.testflowlite.audit.AuditLogService;
import com.testhub.testflowlite.common.ConflictException;
import com.testhub.testflowlite.common.ForbiddenException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.project.Project;
import com.testhub.testflowlite.project.ProjectAccessGuard;
import com.testhub.testflowlite.project.ProjectRepository;
import com.testhub.testflowlite.section.Section;
import com.testhub.testflowlite.section.SectionRepository;
import com.testhub.testflowlite.testcase.*;

import com.testhub.testflowlite.user.User;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final ExcelImportSessionRepository sessionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final SectionRepository sectionRepository;
    private final TestCaseRepository testCaseRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private static final Pattern STEP_NUM_PATTERN = Pattern.compile("^\\s*(?:step\\s*)?(\\d+)\\s*[.):]?\\s*(.*)$", Pattern.CASE_INSENSITIVE);

    @Transactional
    public ExcelImportValidateResponse validateImport(Long projectId, MultipartFile file, String currentUsername) throws IOException {
        User currentUser = projectAccessGuard.verifyProjectAccess(projectId, currentUsername);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        List<ExcelImportRowDto> allRows = new ArrayList<>();
        int errorRowsCount = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            int numberOfSheets = workbook.getNumberOfSheets();

            for (int s = 0; s < numberOfSheets; s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName().trim();

                Row headerRow = sheet.getRow(0);
                String cellA0Text = headerRow != null ? getCellValueAsString(headerRow.getCell(0)).trim() : "";
                boolean isLegacyMode = "Subsection Path".equalsIgnoreCase(cellA0Text);
                SectionPathMode mode = isLegacyMode ? SectionPathMode.LEGACY_SUBSECTION : SectionPathMode.FULL_PATH;

                int lastRow = sheet.getLastRowNum();
                for (int r = 1; r <= lastRow; r++) { // Skip header row 0
                    Row row = sheet.getRow(r);
                    if (row == null || isRowEmpty(row)) {
                        continue;
                    }

                    ExcelImportRowDto rowDto = parseAndValidateRow(row, r + 1, sheetName, mode);
                    if (!rowDto.getErrors().isEmpty()) {
                        errorRowsCount++;
                    }
                    allRows.add(rowDto);
                }
            }
        }

        String importSessionId = UUID.randomUUID().toString();
        List<ExcelImportRowDto> errorRows = allRows.stream().filter(r -> !r.getErrors().isEmpty()).collect(Collectors.toList());

        ExcelImportSession session = new ExcelImportSession();
        session.setImportSessionId(importSessionId);
        session.setProject(projectRepository.getReferenceById(projectId));
        session.setCreatedBy(currentUser);
        session.setParsedPayloadJson(objectMapper.writeValueAsString(allRows));
        session.setErrorLinesJson(objectMapper.writeValueAsString(errorRows));
        session.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        sessionRepository.save(session);
        auditLogService.logAction(currentUser.getId(), "IMPORT_VALIDATE_EXCEL", "PROJECT", projectId, "Validated Excel import with " + allRows.size() + " rows");

        return new ExcelImportValidateResponse(importSessionId, allRows.size(), errorRowsCount, allRows);
    }

    @Transactional
    public ExcelImportConfirmResponse confirmImport(Long projectId, ExcelImportConfirmRequest request, String currentUsername) throws IOException {
        User currentUser = projectAccessGuard.verifyProjectAccess(projectId, currentUsername);

        ExcelImportSession session = sessionRepository.findByImportSessionId(request.getImportSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Import session not found: " + request.getImportSessionId()));

        if (!session.getProject().getId().equals(projectId)) {
            throw new ForbiddenException("Import session does not belong to project " + projectId);
        }

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Import session has expired. Please validate and upload file again.");
        }

        List<ExcelImportRowDto> rows = objectMapper.readValue(session.getParsedPayloadJson(), new TypeReference<>() {});

        // All-or-nothing error check (Rule)
        long hasErrors = rows.stream().filter(r -> r.getErrors() != null && !r.getErrors().isEmpty()).count();
        if (hasErrors > 0) {
            throw new IllegalArgumentException("Cannot confirm import because session contains validation errors. Please fix file and re-upload.");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        Map<String, Integer> casesPerSheet = new LinkedHashMap<>();
        int createdCasesCount = 0;
        int createdSectionsCount = 0;

        // Fetch existing sections for project
        List<Section> existingSections = sectionRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId);

        // Group rows by sheet name
        Map<String, List<ExcelImportRowDto>> sheetGroups = rows.stream().collect(Collectors.groupingBy(ExcelImportRowDto::getSheetName, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<ExcelImportRowDto>> entry : sheetGroups.entrySet()) {
            String sheetName = entry.getKey();
            List<ExcelImportRowDto> sheetRows = entry.getValue();

            int sheetCaseCount = 0;
            List<TestCase> sheetTestCases = new ArrayList<>();

            for (ExcelImportRowDto rowDto : sheetRows) {
                Section startingParent = null;
                if (rowDto.getSectionPathMode() == SectionPathMode.LEGACY_SUBSECTION) {
                    // Legacy mode: Find or create root Section by sheetName
                    Section rootSection = findRootSectionIgnoreCase(existingSections, sheetName);
                    if (rootSection == null) {
                        rootSection = new Section();
                        rootSection.setProject(project);
                        rootSection.setName(sheetName);
                        rootSection.setSortOrder((int) existingSections.stream().filter(s -> s.getParentSection() == null).count());
                        rootSection = sectionRepository.save(rootSection);
                        existingSections.add(rootSection);
                        createdSectionsCount++;
                    }
                    startingParent = rootSection;
                }

                SectionResolveResult resolveResult = resolveTargetSection(project, startingParent, rowDto.getSubsectionPath(), existingSections);
                createdSectionsCount += resolveResult.newlyCreatedCount();

                TestCase tc = new TestCase();
                tc.setSection(resolveResult.section());
                tc.setTitle(rowDto.getTitle());
                tc.setPrecondition(rowDto.getPrecondition());
                tc.setSteps(rowDto.getSteps());
                tc.setExpectedResult(rowDto.getExpectedResult());
                tc.setTestData(rowDto.getTestData());
                tc.setPriority(parsePriorityOrDefault(rowDto.getPriority()));
                tc.setType(parseTypeOrDefault(rowDto.getType()));
                tc.setAutomationStatus(parseAutomationOrDefault(rowDto.getAutomationStatus()));
                tc.setStatus(TestCaseStatus.DRAFT);
                tc.setCreatedBy(currentUser);

                sheetTestCases.add(tc);
            }

            if (!sheetTestCases.isEmpty()) {
                List<TestCase> savedBatch = testCaseRepository.saveAll(sheetTestCases);
                for (TestCase tc : savedBatch) {
                    tc.setCode(String.format("TC-%04d", tc.getId()));
                }
                testCaseRepository.saveAll(savedBatch);
                createdCasesCount += savedBatch.size();
                sheetCaseCount = savedBatch.size();
            }

            casesPerSheet.put(sheetName, sheetCaseCount);
        }

        // Clean up session
        sessionRepository.delete(session);
        auditLogService.logAction(currentUser.getId(), "IMPORT_CONFIRM_EXCEL", "PROJECT", projectId, "Confirmed Excel import creating " + createdCasesCount + " cases");

        return new ExcelImportConfirmResponse(createdCasesCount, createdSectionsCount, casesPerSheet);
    }

    @Transactional(readOnly = true)
    public byte[] generateTemplate(Long projectId, String currentUsername) throws IOException {
        projectAccessGuard.verifyProjectAccess(projectId, currentUsername);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Test Cases");

            // Setup Header Styling
            CellStyle headerStyle = createHeaderCellStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Section Path", "Title", "Precondition", "Steps", "Expected Result",
                    "Test Data", "Priority", "Type", "Automation Status"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fixed Column Widths
            sheet.setColumnWidth(0, 22 * 256); // Section Path
            sheet.setColumnWidth(1, 32 * 256); // Title
            sheet.setColumnWidth(2, 32 * 256); // Precondition
            sheet.setColumnWidth(3, 48 * 256); // Steps
            sheet.setColumnWidth(4, 48 * 256); // Expected Result
            sheet.setColumnWidth(5, 25 * 256); // Test Data
            sheet.setColumnWidth(6, 16 * 256); // Priority
            sheet.setColumnWidth(7, 18 * 256); // Type
            sheet.setColumnWidth(8, 18 * 256); // Automation Status

            sheet.createFreezePane(0, 1);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportCases(Long projectId, List<Long> sectionIds, String currentUsername) throws IOException {
        User currentUser = projectAccessGuard.verifyProjectAccess(projectId, currentUsername);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        List<Section> allSections = sectionRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId);
        List<TestCase> allCases = testCaseRepository.findAll((root, query, cb) ->
                cb.equal(root.get("section").get("project").get("id"), projectId));

        // Group root sections and target cases
        Map<Section, List<TestCase>> rootSectionCaseMap = buildExportStructure(allSections, allCases, sectionIds);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Set<String> usedSheetNames = new HashSet<>();

            for (Map.Entry<Section, List<TestCase>> entry : rootSectionCaseMap.entrySet()) {
                Section rootSec = entry.getKey();
                List<TestCase> sheetCases = entry.getValue();

                String rawSheetName = rootSec.getName();
                String sheetName = sanitizeSheetName(rawSheetName, usedSheetNames);
                usedSheetNames.add(sheetName);

                Sheet sheet = workbook.createSheet(sheetName);

                // Styles
                CellStyle headerStyle = createHeaderCellStyle(workbook);
                CellStyle dataStyle = createDataCellStyle(workbook);

                Row headerRow = sheet.createRow(0);
                String[] headers = {
                        "Section Path", "Title", "Precondition", "Steps", "Expected Result",
                        "Test Data", "Priority", "Type", "Automation Status"
                };

                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Check dynamic column hiding for this sheet
                boolean hideTestData = sheetCases.stream().allMatch(c -> c.getTestData() == null || c.getTestData().trim().isEmpty());
                boolean hideAutomation = sheetCases.stream().allMatch(c -> c.getAutomationStatus() == null || c.getAutomationStatus() == AutomationStatus.MANUAL);

                int rowIndex = 1;
                for (TestCase tc : sheetCases) {
                    Row row = sheet.createRow(rowIndex++);

                    String fullPath = buildFullSectionPath(tc.getSection());

                    createCell(row, 0, fullPath, dataStyle);
                    createCell(row, 1, tc.getTitle(), dataStyle);
                    createCell(row, 2, tc.getPrecondition(), dataStyle);
                    createCell(row, 3, tc.getSteps(), dataStyle);
                    createCell(row, 4, tc.getExpectedResult(), dataStyle);
                    createCell(row, 5, tc.getTestData(), dataStyle);
                    createCell(row, 6, formatEnumValue(tc.getPriority()), dataStyle);
                    createCell(row, 7, formatEnumValue(tc.getType()), dataStyle);
                    createCell(row, 8, formatEnumValue(tc.getAutomationStatus()), dataStyle);

                    // Calculate dynamic row height
                    int maxLines = getMaxLines(fullPath, tc.getTitle(), tc.getPrecondition(), tc.getSteps(), tc.getExpectedResult(), tc.getTestData());
                    row.setHeightInPoints((maxLines + 1) * 15f);
                }

                // Fixed Column Widths
                sheet.setColumnWidth(0, 22 * 256);
                sheet.setColumnWidth(1, 32 * 256);
                sheet.setColumnWidth(2, 32 * 256);
                sheet.setColumnWidth(3, 48 * 256);
                sheet.setColumnWidth(4, 48 * 256);
                sheet.setColumnWidth(5, 25 * 256);
                sheet.setColumnWidth(6, 16 * 256);
                sheet.setColumnWidth(7, 18 * 256);
                sheet.setColumnWidth(8, 18 * 256);

                sheet.createFreezePane(0, 1);

                if (hideTestData) {
                    sheet.setColumnHidden(5, true);
                }
                if (hideAutomation) {
                    sheet.setColumnHidden(8, true);
                }
            }

            auditLogService.logAction(currentUser.getId(), "EXPORT_EXCEL", "PROJECT", projectId, "Exported project test cases to Excel");
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private ExcelImportRowDto parseAndValidateRow(Row row, int rowNum, String sheetName, SectionPathMode mode) {
        ExcelImportRowDto dto = new ExcelImportRowDto();
        dto.setRowNumber(rowNum);
        dto.setSheetName(sheetName);
        dto.setSectionPathMode(mode);

        dto.setSubsectionPath(getCellValueAsString(row.getCell(0)));
        dto.setTitle(getCellValueAsString(row.getCell(1)));
        dto.setPrecondition(getCellValueAsString(row.getCell(2)));
        dto.setSteps(getCellValueAsString(row.getCell(3)));
        dto.setExpectedResult(getCellValueAsString(row.getCell(4)));
        dto.setTestData(getCellValueAsString(row.getCell(5)));
        dto.setPriority(getCellValueAsString(row.getCell(6)));
        dto.setType(getCellValueAsString(row.getCell(7)));
        dto.setAutomationStatus(getCellValueAsString(row.getCell(8)));

        List<String> errors = new ArrayList<>();

        if (dto.getTitle().isBlank()) errors.add("Title is required (Column B)");
        if (dto.getPrecondition().isBlank()) errors.add("Precondition is required (Column C)");
        if (dto.getSteps().isBlank()) errors.add("Steps are required (Column D)");
        if (dto.getExpectedResult().isBlank()) errors.add("Expected Result is required (Column E)");

        // Validate Enum values if provided
        if (!dto.getPriority().isBlank() && parsePriority(dto.getPriority()) == null) {
            errors.add("Invalid Priority '" + dto.getPriority() + "' (Column G)");
        }
        if (!dto.getType().isBlank() && parseType(dto.getType()) == null) {
            errors.add("Invalid Test Type '" + dto.getType() + "' (Column H)");
        }
        if (!dto.getAutomationStatus().isBlank() && parseAutomation(dto.getAutomationStatus()) == null) {
            errors.add("Invalid Automation Status '" + dto.getAutomationStatus() + "' (Column I)");
        }

        // Validate Step & Expected Result Numbered Correspondence
        if (!dto.getSteps().isBlank() && !dto.getExpectedResult().isBlank()) {
            Set<Integer> stepNumbers = extractStepNumbers(dto.getSteps());
            Set<Integer> resultNumbers = extractStepNumbers(dto.getExpectedResult());

            for (Integer num : resultNumbers) {
                if (!stepNumbers.contains(num)) {
                    errors.add("Expected Result references step " + num + " which does not exist in Steps");
                }
            }
        }

        dto.setErrors(errors);
        return dto;
    }

    private Set<Integer> extractStepNumbers(String text) {
        Set<Integer> numbers = new HashSet<>();
        if (text == null || text.isBlank()) return numbers;

        String[] lines = text.split("\r?\n");
        for (String line : lines) {
            Matcher m = STEP_NUM_PATTERN.matcher(line);
            if (m.find()) {
                try {
                    numbers.add(Integer.parseInt(m.group(1)));
                } catch (NumberFormatException ignored) {}
            }
        }
        return numbers;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.STRING) return cell.getStringCellValue().trim();
        if (type == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue()).trim();
        if (type == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue()).trim();
        if (type == CellType.FORMULA) {
            try {
                return cell.getStringCellValue().trim();
            } catch (Exception e) {
                return String.valueOf((long) cell.getNumericCellValue()).trim();
            }
        }
        return "";
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValueAsString(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private Section findRootSectionIgnoreCase(List<Section> sections, String name) {
        return sections.stream()
                .filter(s -> s.getParentSection() == null && s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private record SectionResolveResult(Section section, int newlyCreatedCount) {}

    private SectionResolveResult resolveTargetSection(Project project, Section startingParent, String path, List<Section> allSections) {
        String trimmedPath = (path != null) ? path.trim() : "";

        if (startingParent != null && trimmedPath.isEmpty()) {
            return new SectionResolveResult(startingParent, 0);
        }

        if (startingParent == null && trimmedPath.isEmpty()) {
            Section uncategorized = findRootSectionIgnoreCase(allSections, "Uncategorized");
            int newlyCreated = 0;
            if (uncategorized == null) {
                uncategorized = new Section();
                uncategorized.setProject(project);
                uncategorized.setName("Uncategorized");
                uncategorized.setSortOrder((int) allSections.stream().filter(s -> s.getParentSection() == null).count());
                uncategorized = sectionRepository.save(uncategorized);
                allSections.add(uncategorized);
                newlyCreated = 1;
            }
            return new SectionResolveResult(uncategorized, newlyCreated);
        }

        String[] parts = trimmedPath.split(">");
        Section currentParent = startingParent;
        int newlyCreated = 0;

        for (String part : parts) {
            String segName = part.trim();
            if (segName.isEmpty()) continue;

            Section found = null;
            if (currentParent == null) {
                found = findRootSectionIgnoreCase(allSections, segName);
            } else {
                final Long parentId = currentParent.getId();
                for (Section s : allSections) {
                    if (s.getParentSection() != null && s.getParentSection().getId().equals(parentId) && s.getName().equalsIgnoreCase(segName)) {
                        found = s;
                        break;
                    }
                }
            }

            if (found == null) {
                Section newSec = new Section();
                newSec.setProject(project);
                newSec.setName(segName);

                if (currentParent == null) {
                    newSec.setParentSection(null);
                    int rootCount = (int) allSections.stream().filter(s -> s.getParentSection() == null).count();
                    newSec.setSortOrder(rootCount);
                } else {
                    newSec.setParentSection(currentParent);
                    final Long parentId = currentParent.getId();
                    int childCount = (int) allSections.stream()
                            .filter(s -> s.getParentSection() != null && s.getParentSection().getId().equals(parentId))
                            .count();
                    newSec.setSortOrder(childCount);
                }

                newSec = sectionRepository.save(newSec);
                allSections.add(newSec);
                currentParent = newSec;
                newlyCreated++;
            } else {
                currentParent = found;
            }
        }

        return new SectionResolveResult(currentParent, newlyCreated);
    }

    private Priority parsePriority(String val) {
        for (Priority p : Priority.values()) {
            if (p.name().equalsIgnoreCase(val)) return p;
        }
        return null;
    }

    private Priority parsePriorityOrDefault(String val) {
        Priority p = parsePriority(val);
        return p != null ? p : Priority.MEDIUM;
    }

    private TestCaseType parseType(String val) {
        for (TestCaseType t : TestCaseType.values()) {
            if (t.name().equalsIgnoreCase(val)) return t;
        }
        return null;
    }

    private TestCaseType parseTypeOrDefault(String val) {
        TestCaseType t = parseType(val);
        return t != null ? t : TestCaseType.FUNCTIONAL;
    }

    private AutomationStatus parseAutomation(String val) {
        for (AutomationStatus a : AutomationStatus.values()) {
            if (a.name().equalsIgnoreCase(val) || a.name().replace("_", " ").equalsIgnoreCase(val)) return a;
        }
        return null;
    }

    private AutomationStatus parseAutomationOrDefault(String val) {
        AutomationStatus a = parseAutomation(val);
        return a != null ? a : AutomationStatus.MANUAL;
    }

    private String sanitizeSheetName(String rawName, Set<String> usedNames) {
        String name = rawName.replaceAll("[\\\\/*?:\\[\\]]", "").trim();
        if (name.length() > 31) {
            name = name.substring(0, 31);
        }
        if (name.isEmpty()) name = "Section";

        if (!usedNames.contains(name)) {
            return name;
        }

        int suffix = 1;
        while (true) {
            String candidate = name.length() > 28 ? name.substring(0, 28) + "~" + suffix : name + "~" + suffix;
            if (!usedNames.contains(candidate)) {
                return candidate;
            }
            suffix++;
        }
    }

    private Map<Section, List<TestCase>> buildExportStructure(List<Section> allSections, List<TestCase> allCases, List<Long> targetSectionIds) {
        Map<Section, List<TestCase>> result = new LinkedHashMap<>();

        // Group cases by Section ID
        Map<Long, List<TestCase>> casesBySectionId = allCases.stream().collect(Collectors.groupingBy(c -> c.getSection().getId()));

        Set<Long> targetSet = (targetSectionIds != null && !targetSectionIds.isEmpty()) ? new HashSet<>(targetSectionIds) : null;

        // Root sections
        List<Section> rootSections = allSections.stream().filter(s -> s.getParentSection() == null).collect(Collectors.toList());

        for (Section rootSec : rootSections) {
            List<TestCase> rootTreeCases = new ArrayList<>();
            collectCasesInTree(rootSec, allSections, casesBySectionId, targetSet, rootTreeCases);

            if (!rootTreeCases.isEmpty() || targetSet == null) {
                result.put(rootSec, rootTreeCases);
            }
        }

        return result;
    }

    private void collectCasesInTree(Section current, List<Section> allSections, Map<Long, List<TestCase>> casesBySectionId, Set<Long> targetSet, List<TestCase> collector) {
        boolean includeThisSection = (targetSet == null || targetSet.contains(current.getId()));

        if (includeThisSection && casesBySectionId.containsKey(current.getId())) {
            collector.addAll(casesBySectionId.get(current.getId()));
        }

        List<Section> children = allSections.stream().filter(s -> s.getParentSection() != null && s.getParentSection().getId().equals(current.getId())).collect(Collectors.toList());

        for (Section child : children) {
            // If parent section was selected in filter, pass targetSet as null down to child to include full subtree
            Set<Long> nextTargetSet = includeThisSection ? null : targetSet;
            collectCasesInTree(child, allSections, casesBySectionId, nextTargetSet, collector);
        }
    }

    private String buildFullSectionPath(Section targetSec) {
        if (targetSec == null) return "";
        List<String> names = new ArrayList<>();
        Section curr = targetSec;
        while (curr != null) {
            names.add(curr.getName());
            curr = curr.getParentSection();
        }
        Collections.reverse(names);
        return String.join(" > ", names);
    }

    private String buildRelativeSubsectionPath(Section rootSec, Section targetSec) {
        if (targetSec.getId().equals(rootSec.getId())) {
            return "";
        }

        List<String> names = new ArrayList<>();
        Section curr = targetSec;
        while (curr != null && !curr.getId().equals(rootSec.getId())) {
            names.add(curr.getName());
            curr = curr.getParentSection();
        }
        Collections.reverse(names);
        return String.join(" > ", names);
    }

    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints((short) 13);
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());

        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        if (workbook instanceof XSSFWorkbook) {
            ((org.apache.poi.xssf.usermodel.XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(new byte[]{(byte) 217, (byte) 226, (byte) 243}, null));
        } else {
            style.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
        }

        return style;
    }

    private CellStyle createDataCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints((short) 13);

        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        return style;
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private int getMaxLines(String... texts) {
        int max = 1;
        for (String text : texts) {
            if (text != null && !text.isEmpty()) {
                int lines = text.split("\r?\n").length;
                if (lines > max) max = lines;
            }
        }
        return max;
    }

    private String formatEnumValue(Enum<?> enumVal) {
        if (enumVal == null) return "";
        String name = enumVal.name();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
