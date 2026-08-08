package com.testhub.testflowlite.excel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.project.Project;
import com.testhub.testflowlite.project.ProjectMember;
import com.testhub.testflowlite.project.ProjectMemberRepository;
import com.testhub.testflowlite.project.ProjectRepository;
import com.testhub.testflowlite.section.Section;
import com.testhub.testflowlite.section.SectionRepository;
import com.testhub.testflowlite.security.JwtTokenProvider;
import com.testhub.testflowlite.testcase.TestCase;
import com.testhub.testflowlite.testcase.TestCaseRepository;
import com.testhub.testflowlite.testcase.TestCaseStatus;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ExcelControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testhub_db_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private ExcelImportSessionRepository sessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User leader;
    private User tester;
    private Project project;
    private String leaderToken;
    private String testerToken;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
        testCaseRepository.deleteAll();
        sectionRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        leader = new User();
        leader.setUsername("leader");
        leader.setEmail("leader@testhub.com");
        leader.setPasswordHash(passwordEncoder.encode("Leader@123456"));
        leader.setFullName("System Leader");
        leader.setRole(Role.LEADER);
        leader.setIsActive(true);
        leader = userRepository.save(leader);

        tester = new User();
        tester.setUsername("tester");
        tester.setEmail("tester@testhub.com");
        tester.setPasswordHash(passwordEncoder.encode("Tester@123456"));
        tester.setFullName("Assigned Tester");
        tester.setRole(Role.TESTER);
        tester.setIsActive(true);
        tester = userRepository.save(tester);

        project = new Project();
        project.setName("Excel Integration Project");
        project.setCreatedBy(leader);
        project = projectRepository.save(project);

        ProjectMember pm = new ProjectMember();
        pm.setProject(project);
        pm.setUser(tester);
        projectMemberRepository.save(pm);

        leaderToken = jwtTokenProvider.generateAccessToken("leader", "LEADER");
        testerToken = jwtTokenProvider.generateAccessToken("tester", "TESTER");
    }

    @Test
    void testValidateImport_ValidFile_Success() throws Exception {
        byte[] excelBytes = createSampleExcelFile("Authentication Module", "Login Sub", "Valid Login Test", "Precondition", "1. Step 1", "1. Expected 1", "Data", "High", "Functional", "Manual");

        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        mockMvc.perform(multipart("/api/projects/" + project.getId() + "/cases/import/validate")
                        .file(file)
                        .header("Authorization", "Bearer " + testerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRows").value(1))
                .andExpect(jsonPath("$.data.errorRowsCount").value(0))
                .andExpect(jsonPath("$.data.importSessionId").exists());
    }

    @Test
    void testValidateImport_InvalidStepReference_ReturnsRowError() throws Exception {
        // Expected result references Step 2 which does not exist in Steps (only step 1)
        byte[] excelBytes = createSampleExcelFile("Auth", "", "Invalid Step Ref Case", "Precondition", "1. Step one", "2. Expected step two", "Data", "Medium", "Functional", "Manual");

        MockMultipartFile file = new MockMultipartFile("file", "test_invalid.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        mockMvc.perform(multipart("/api/projects/" + project.getId() + "/cases/import/validate")
                        .file(file)
                        .header("Authorization", "Bearer " + testerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.errorRowsCount").value(1))
                .andExpect(jsonPath("$.data.rows[0].errors[0]").value("Expected Result references step 2 which does not exist in Steps"));
    }

    @Test
    void testConfirmImport_ValidSession_CreatesCasesAndSections() throws Exception {
        byte[] excelBytes = createSampleExcelFile("User Management", "Profile > Settings", "Update Profile Avatar", "User logged in", "1. Go to profile\n2. Upload avatar", "1. Profile loaded\n2. Avatar updated", "Avatar.png", "High", "Functional", "Manual");

        MockMultipartFile file = new MockMultipartFile("file", "confirm_test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        MvcResult validateResult = mockMvc.perform(multipart("/api/projects/" + project.getId() + "/cases/import/validate")
                        .file(file)
                        .header("Authorization", "Bearer " + testerToken))
                .andExpect(status().isOk())
                .andReturn();

        String json = validateResult.getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(json).get("data").get("importSessionId").asText();

        ExcelImportConfirmRequest req = new ExcelImportConfirmRequest(sessionId);

        mockMvc.perform(post("/api/projects/" + project.getId() + "/cases/import/confirm")
                        .header("Authorization", "Bearer " + testerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdCasesCount").value(1))
                .andExpect(jsonPath("$.data.createdSectionsCount").value(3)); // User Management (Root) -> Profile -> Settings

        // Verify cases in DB
        List<TestCase> cases = testCaseRepository.findAll();
        assertEquals(1, cases.size());
        assertEquals("Update Profile Avatar", cases.get(0).getTitle());
        assertEquals(TestCaseStatus.DRAFT, cases.get(0).getStatus());
        assertEquals("Settings", cases.get(0).getSection().getName());
    }

    @Test
    void testConfirmImport_WithErrors_BadRequest400() throws Exception {
        // Invalid file with missing Title
        byte[] excelBytes = createSampleExcelFile("Auth", "", "", "Precondition", "1. Step", "1. Expected", "", "High", "Functional", "Manual");

        MockMultipartFile file = new MockMultipartFile("file", "err.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        MvcResult validateResult = mockMvc.perform(multipart("/api/projects/" + project.getId() + "/cases/import/validate")
                        .file(file)
                        .header("Authorization", "Bearer " + testerToken))
                .andExpect(status().isOk())
                .andReturn();

        String sessionId = objectMapper.readTree(validateResult.getResponse().getContentAsString()).get("data").get("importSessionId").asText();
        ExcelImportConfirmRequest req = new ExcelImportConfirmRequest(sessionId);

        mockMvc.perform(post("/api/projects/" + project.getId() + "/cases/import/confirm")
                        .header("Authorization", "Bearer " + testerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("validation errors")));
    }

    @Test
    void testDownloadTemplate_Success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/projects/" + project.getId() + "/cases/import/template")
                        .header("Authorization", "Bearer " + testerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("TestHub_Import_Template.xlsx")))
                .andReturn();

        byte[] templateBytes = result.getResponse().getContentAsByteArray();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(templateBytes))) {
            assertEquals(1, wb.getNumberOfSheets());
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            assertEquals("Subsection Path", headerRow.getCell(0).getStringCellValue());
            assertEquals("Title", headerRow.getCell(1).getStringCellValue());
        }
    }

    @Test
    void testExportCases_Success() throws Exception {
        Section sec = new Section();
        sec.setProject(project);
        sec.setName("Export Root Section");
        sec = sectionRepository.save(sec);

        TestCase tc = new TestCase();
        tc.setSection(sec);
        tc.setTitle("Export Test Case");
        tc.setSteps("1. Step");
        tc.setExpectedResult("1. Result");
        tc.setCreatedBy(leader);
        tc = testCaseRepository.save(tc);
        tc.setCode("TC-0001");
        testCaseRepository.save(tc);

        MvcResult result = mockMvc.perform(get("/api/projects/" + project.getId() + "/cases/export")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("TestHub_Export_Project_")))
                .andReturn();

        byte[] exportBytes = result.getResponse().getContentAsByteArray();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(exportBytes))) {
            assertEquals(1, wb.getNumberOfSheets());
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("Export Root Section", sheet.getSheetName());
            Row dataRow = sheet.getRow(1);
            assertEquals("Export Test Case", dataRow.getCell(1).getStringCellValue());
        }
    }

    private byte[] createSampleExcelFile(String sheetName, String subPath, String title, String precondition, String steps, String expected, String testData, String priority, String type, String automation) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Subsection Path");
            header.createCell(1).setCellValue("Title");
            header.createCell(2).setCellValue("Precondition");
            header.createCell(3).setCellValue("Steps");
            header.createCell(4).setCellValue("Expected Result");
            header.createCell(5).setCellValue("Test Data");
            header.createCell(6).setCellValue("Priority");
            header.createCell(7).setCellValue("Type");
            header.createCell(8).setCellValue("Automation Status");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(subPath);
            row.createCell(1).setCellValue(title);
            row.createCell(2).setCellValue(precondition);
            row.createCell(3).setCellValue(steps);
            row.createCell(4).setCellValue(expected);
            row.createCell(5).setCellValue(testData);
            row.createCell(6).setCellValue(priority);
            row.createCell(7).setCellValue(type);
            row.createCell(8).setCellValue(automation);

            wb.write(out);
            return out.toByteArray();
        }
    }
}
