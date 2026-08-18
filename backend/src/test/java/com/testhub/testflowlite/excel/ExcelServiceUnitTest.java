package com.testhub.testflowlite.excel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testhub.testflowlite.audit.AuditLogService;
import com.testhub.testflowlite.common.ForbiddenException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.project.Project;
import com.testhub.testflowlite.project.ProjectAccessGuard;
import com.testhub.testflowlite.project.ProjectMemberRepository;
import com.testhub.testflowlite.project.ProjectRepository;
import com.testhub.testflowlite.section.Section;
import com.testhub.testflowlite.section.SectionRepository;
import com.testhub.testflowlite.testcase.TestCase;
import com.testhub.testflowlite.testcase.TestCaseRepository;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExcelServiceUnitTest {

    @Mock
    private ExcelImportSessionRepository sessionRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuditLogService auditLogService;
    private ProjectAccessGuard projectAccessGuard;
    private ExcelService excelService;

    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(null) {
            @Override
            public void logAction(Long userId, String action, String entityType, Long entityId, String detailJson) {
                // No-op for unit tests
            }
        };
        projectAccessGuard = new ProjectAccessGuard(projectRepository, projectMemberRepository, userRepository);
        excelService = new ExcelService(
                sessionRepository,
                projectRepository,
                projectAccessGuard,
                sectionRepository,
                testCaseRepository,
                auditLogService,
                objectMapper
        );

        user = new User();
        user.setId(1L);
        user.setUsername("tester");
        user.setRole(Role.LEADER);

        project = new Project();
        project.setId(10L);
        project.setName("Unit Project");

        lenient().when(projectRepository.existsById(10L)).thenReturn(true);
        lenient().when(userRepository.findByUsernameOrEmail("tester", "tester")).thenReturn(Optional.of(user));
        lenient().when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        lenient().when(projectRepository.getReferenceById(10L)).thenReturn(project);
    }

    @Test
    void testGenerateTemplate_HasSectionPathHeader() throws Exception {
        byte[] templateBytes = excelService.generateTemplate(10L, "tester");
        assertNotNull(templateBytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(templateBytes))) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            assertEquals("Section Path", headerRow.getCell(0).getStringCellValue());
            assertEquals("Title", headerRow.getCell(1).getStringCellValue());
        }
    }

    @Test
    void testGenerateTemplate_NonExistentProject_ThrowsResourceNotFound() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> excelService.generateTemplate(999L, "tester")
        );
    }

    @Test
    void testValidateImport_DetectsFullPathAndLegacyModesPerSheet() throws Exception {
        byte[] excelBytes;
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet s1 = wb.createSheet("Sheet 1");
            Row h1 = s1.createRow(0);
            h1.createCell(0).setCellValue("Section Path");
            h1.createCell(1).setCellValue("Title 1");
            h1.createCell(2).setCellValue("Pre 1");
            h1.createCell(3).setCellValue("1. Step");
            h1.createCell(4).setCellValue("1. Res");

            Row r1 = s1.createRow(1);
            r1.createCell(0).setCellValue("Payment > Checkout");
            r1.createCell(1).setCellValue("Case 1");
            r1.createCell(2).setCellValue("Pre 1");
            r1.createCell(3).setCellValue("1. Step");
            r1.createCell(4).setCellValue("1. Res");

            Sheet s2 = wb.createSheet("Legacy Sheet");
            Row h2 = s2.createRow(0);
            h2.createCell(0).setCellValue("Subsection Path");
            h2.createCell(1).setCellValue("Title 2");
            h2.createCell(2).setCellValue("Pre 2");
            h2.createCell(3).setCellValue("1. Step");
            h2.createCell(4).setCellValue("1. Res");

            Row r2 = s2.createRow(1);
            r2.createCell(0).setCellValue("Child Sub");
            r2.createCell(1).setCellValue("Case 2");
            r2.createCell(2).setCellValue("Pre 2");
            r2.createCell(3).setCellValue("1. Step");
            r2.createCell(4).setCellValue("1. Res");

            wb.write(out);
            excelBytes = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        ExcelImportValidateResponse resp = excelService.validateImport(10L, file, "tester");
        assertNotNull(resp.getImportSessionId());
        assertEquals(2, resp.getTotalRows());
        assertEquals(0, resp.getErrorRowsCount());
        assertEquals(SectionPathMode.FULL_PATH, resp.getRows().get(0).getSectionPathMode());
        assertEquals(SectionPathMode.LEGACY_SUBSECTION, resp.getRows().get(1).getSectionPathMode());
    }

    @Test
    void testValidateImport_NonExistentProject_ThrowsResourceNotFound() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[10]);

        assertThrows(
                ResourceNotFoundException.class,
                () -> excelService.validateImport(999L, file, "tester")
        );

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void testConfirmImport_ThrowsForbiddenWhenSessionProjectMismatch() throws Exception {
        Project projectA = new Project();
        projectA.setId(10L);
        projectA.setName("Project A");

        ExcelImportSession session = new ExcelImportSession();
        session.setImportSessionId("session-project-a");
        session.setProject(projectA);
        session.setParsedPayloadJson("[]");
        session.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(sessionRepository.findByImportSessionId("session-project-a")).thenReturn(Optional.of(session));
        when(projectRepository.existsById(20L)).thenReturn(true);

        // Attempting to confirm session for Project A (10L) into Project B (20L)
        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> excelService.confirmImport(20L, new ExcelImportConfirmRequest("session-project-a"), "tester")
        );

        assertEquals("Import session does not belong to project 20", ex.getMessage());
        verify(testCaseRepository, never()).saveAll(anyList());
        verify(sectionRepository, never()).save(any(Section.class));
    }

    @Test
    void testConfirmImport_FullPath_EmptyPath_ResolvesUncategorized() throws Exception {
        ExcelImportRowDto rowDto = new ExcelImportRowDto();
        rowDto.setRowNumber(2);
        rowDto.setSheetName("Tab 1");
        rowDto.setSubsectionPath("");
        rowDto.setTitle("Uncategorized Test");
        rowDto.setPrecondition("Pre");
        rowDto.setSteps("1. Step");
        rowDto.setExpectedResult("1. Res");
        rowDto.setSectionPathMode(SectionPathMode.FULL_PATH);

        ExcelImportSession session = new ExcelImportSession();
        session.setImportSessionId("session-uncategorized");
        session.setProject(project);
        session.setParsedPayloadJson(objectMapper.writeValueAsString(List.of(rowDto)));
        session.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(sessionRepository.findByImportSessionId("session-uncategorized")).thenReturn(Optional.of(session));
        when(sectionRepository.findByProjectIdOrderBySortOrderAscIdAsc(10L)).thenReturn(new ArrayList<>());
        when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> {
            Section s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });
        when(testCaseRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<TestCase> list = inv.getArgument(0);
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setId((long) (i + 1));
            }
            return list;
        });

        ExcelImportConfirmResponse response = excelService.confirmImport(10L, new ExcelImportConfirmRequest("session-uncategorized"), "tester");

        assertEquals(1, response.getCreatedCasesCount());
        assertEquals(1, response.getCreatedSectionsCount());
        verify(sectionRepository).save(argThat(s -> "Uncategorized".equals(s.getName()) && s.getParentSection() == null));
    }

    @Test
    void testConfirmImport_LegacyMode_ResolvesSheetNameAsRoot() throws Exception {
        ExcelImportRowDto rowDto = new ExcelImportRowDto();
        rowDto.setRowNumber(2);
        rowDto.setSheetName("User Management");
        rowDto.setSubsectionPath("Profile > Settings");
        rowDto.setTitle("Legacy Test");
        rowDto.setPrecondition("Pre");
        rowDto.setSteps("1. Step");
        rowDto.setExpectedResult("1. Res");
        rowDto.setSectionPathMode(SectionPathMode.LEGACY_SUBSECTION);

        ExcelImportSession session = new ExcelImportSession();
        session.setImportSessionId("session-legacy");
        session.setProject(project);
        session.setParsedPayloadJson(objectMapper.writeValueAsString(List.of(rowDto)));
        session.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        List<Section> existingSections = new ArrayList<>();

        when(sessionRepository.findByImportSessionId("session-legacy")).thenReturn(Optional.of(session));
        when(sectionRepository.findByProjectIdOrderBySortOrderAscIdAsc(10L)).thenReturn(existingSections);
        when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> {
            Section s = inv.getArgument(0);
            s.setId((long) (existingSections.size() + 10));
            return s;
        });
        when(testCaseRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ExcelImportConfirmResponse response = excelService.confirmImport(10L, new ExcelImportConfirmRequest("session-legacy"), "tester");

        assertEquals(1, response.getCreatedCasesCount());
        assertEquals(3, response.getCreatedSectionsCount()); // User Management (Root) -> Profile -> Settings
    }

    @Test
    void testExportCases_OutputsFullSectionPath() throws Exception {
        Section rootSec = new Section();
        rootSec.setId(1L);
        rootSec.setName("Payment Module");
        rootSec.setProject(project);

        Section childSec = new Section();
        childSec.setId(2L);
        childSec.setName("Checkout Page");
        childSec.setParentSection(rootSec);
        childSec.setProject(project);

        TestCase tc = new TestCase();
        tc.setId(5L);
        tc.setCode("TC-0005");
        tc.setTitle("Checkout Test");
        tc.setSection(childSec);
        tc.setSteps("1. Step");
        tc.setExpectedResult("1. Res");

        when(sectionRepository.findByProjectIdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(rootSec, childSec));
        when(testCaseRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of(tc));

        byte[] exportedBytes = excelService.exportCases(10L, null, "tester");
        assertNotNull(exportedBytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(exportedBytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("Payment Module", sheet.getSheetName());
            Row headerRow = sheet.getRow(0);
            assertEquals("Section Path", headerRow.getCell(0).getStringCellValue());
            Row dataRow = sheet.getRow(1);
            assertEquals("Payment Module > Checkout Page", dataRow.getCell(0).getStringCellValue());
            assertEquals("Checkout Test", dataRow.getCell(1).getStringCellValue());
        }
    }
}
