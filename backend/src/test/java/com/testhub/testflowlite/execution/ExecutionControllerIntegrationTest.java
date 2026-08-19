package com.testhub.testflowlite.execution;

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
import com.testhub.testflowlite.testrun.*;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
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

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ExecutionControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testhub_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mysql.getJdbcUrl() + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private TestRunRepository testRunRepository;

    @Autowired
    private TestRunCaseRepository testRunCaseRepository;

    @Autowired
    private ExecutionHistoryRepository executionHistoryRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User leader;
    private User assignedTester;
    private User unassignedTester;
    private User externalTester;
    private String leaderToken;
    private String assignedTesterToken;
    private String unassignedTesterToken;
    private String externalTesterToken;

    private Project project;
    private Project externalProject;
    private TestCase testCase;
    private TestRun openRun;
    private TestRun closedRun;

    @BeforeEach
    void setUp() {
        executionHistoryRepository.deleteAll();
        testRunCaseRepository.deleteAll();
        testRunRepository.deleteAll();
        testCaseRepository.deleteAll();
        sectionRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        leader = new User();
        leader.setUsername("leader_exec");
        leader.setEmail("leader_exec@example.com");
        leader.setPasswordHash(passwordEncoder.encode("password"));
        leader.setFullName("Leader Exec");
        leader.setRole(Role.LEADER);
        leader.setIsActive(true);
        leader = userRepository.save(leader);

        assignedTester = new User();
        assignedTester.setUsername("tester_assigned");
        assignedTester.setEmail("tester_assigned@example.com");
        assignedTester.setPasswordHash(passwordEncoder.encode("password"));
        assignedTester.setFullName("Assigned Tester");
        assignedTester.setRole(Role.TESTER);
        assignedTester.setIsActive(true);
        assignedTester = userRepository.save(assignedTester);

        unassignedTester = new User();
        unassignedTester.setUsername("tester_unassigned");
        unassignedTester.setEmail("tester_unassigned@example.com");
        unassignedTester.setPasswordHash(passwordEncoder.encode("password"));
        unassignedTester.setFullName("Unassigned Tester");
        unassignedTester.setRole(Role.TESTER);
        unassignedTester.setIsActive(true);
        unassignedTester = userRepository.save(unassignedTester);

        externalTester = new User();
        externalTester.setUsername("tester_external");
        externalTester.setEmail("tester_external@example.com");
        externalTester.setPasswordHash(passwordEncoder.encode("password"));
        externalTester.setFullName("External Tester");
        externalTester.setRole(Role.TESTER);
        externalTester.setIsActive(true);
        externalTester = userRepository.save(externalTester);

        leaderToken = jwtTokenProvider.generateAccessToken(leader.getUsername(), leader.getRole().name());
        assignedTesterToken = jwtTokenProvider.generateAccessToken(assignedTester.getUsername(), assignedTester.getRole().name());
        unassignedTesterToken = jwtTokenProvider.generateAccessToken(unassignedTester.getUsername(), unassignedTester.getRole().name());
        externalTesterToken = jwtTokenProvider.generateAccessToken(externalTester.getUsername(), externalTester.getRole().name());

        project = new Project();
        project.setName("Execution Project");
        project.setCreatedBy(leader);
        project = projectRepository.save(project);

        ProjectMember pm1 = new ProjectMember();
        pm1.setProject(project);
        pm1.setUser(assignedTester);
        projectMemberRepository.save(pm1);

        ProjectMember pm2 = new ProjectMember();
        pm2.setProject(project);
        pm2.setUser(unassignedTester);
        projectMemberRepository.save(pm2);

        externalProject = new Project();
        externalProject.setName("External Project");
        externalProject.setCreatedBy(leader);
        externalProject = projectRepository.save(externalProject);

        ProjectMember pmExt = new ProjectMember();
        pmExt.setProject(externalProject);
        pmExt.setUser(externalTester);
        projectMemberRepository.save(pmExt);

        Section section = new Section();
        section.setName("Exec Section");
        section.setProject(project);
        section = sectionRepository.save(section);

        testCase = new TestCase();
        testCase.setTitle("Login Test Case");
        testCase.setSection(section);
        testCase.setSteps("1. Open page");
        testCase.setExpectedResult("1. Page opened");
        testCase.setStatus(TestCaseStatus.READY);
        testCase.setCreatedBy(leader);
        testCase = testCaseRepository.save(testCase);
        testCase.setCode(String.format("TC-%04d", testCase.getId()));
        testCase = testCaseRepository.save(testCase);

        openRun = new TestRun();
        openRun.setName("Open Execution Run");
        openRun.setProject(project);
        openRun.setCreatedBy(leader);
        openRun.setStatus(RunStatus.OPEN);
        openRun = testRunRepository.save(openRun);

        TestRunCase trc = new TestRunCase();
        trc.setRun(openRun);
        trc.setCaseId(testCase.getId());
        trc.setTitle(testCase.getTitle());
        trc.setSteps(testCase.getSteps());
        trc.setExpectedResult(testCase.getExpectedResult());
        trc.setAssignedTo(assignedTester);
        trc.setResultStatus(ResultStatus.UNTESTED);
        trc.setIsReviewed(false);
        testRunCaseRepository.save(trc);

        closedRun = new TestRun();
        closedRun.setName("Closed Execution Run");
        closedRun.setProject(project);
        closedRun.setCreatedBy(leader);
        closedRun.setStatus(RunStatus.CLOSED);
        closedRun = testRunRepository.save(closedRun);

        TestRunCase closedTrc = new TestRunCase();
        closedTrc.setRun(closedRun);
        closedTrc.setCaseId(testCase.getId());
        closedTrc.setTitle(testCase.getTitle());
        closedTrc.setSteps(testCase.getSteps());
        closedTrc.setExpectedResult(testCase.getExpectedResult());
        closedTrc.setAssignedTo(assignedTester);
        closedTrc.setResultStatus(ResultStatus.UNTESTED);
        closedTrc.setIsReviewed(false);
        testRunCaseRepository.save(closedTrc);
    }

    @Test
    void testRecordExecution_AssignedTester_ReturnsLatestHistoryId() throws Exception {
        ExecutionDto dto = new ExecutionDto();
        dto.setResultStatus(ResultStatus.PASSED);
        dto.setComment("Passed cleanly");
        dto.setDefectRef("JIRA-101");

        mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/execute")
                        .header("Authorization", "Bearer " + assignedTesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultStatus").value("PASSED"))
                .andExpect(jsonPath("$.data.executedBy").value("Assigned Tester"))
                .andExpect(jsonPath("$.data.isReviewed").value(false))
                .andExpect(jsonPath("$.data.latestExecutionHistoryId", notNullValue()));

        TestRunCase updatedCase = testRunCaseRepository.findByRunIdAndCaseId(openRun.getId(), testCase.getId()).orElseThrow();
        assertEquals(ResultStatus.PASSED, updatedCase.getResultStatus());
        assertFalse(updatedCase.getIsReviewed());

        List<ExecutionHistory> history = executionHistoryRepository.findByRunCaseIdOrderByExecutedAtDesc(updatedCase.getId());
        assertEquals(1, history.size());
        assertEquals(ResultStatus.PASSED, history.get(0).getResultStatus());
    }

    @Test
    void testRecordExecution_MissingResultStatus_Returns400() throws Exception {
        ExecutionDto dto = new ExecutionDto();
        dto.setComment("Missing status");

        mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/execute")
                        .header("Authorization", "Bearer " + assignedTesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRecordExecution_UnassignedTester_Returns403() throws Exception {
        ExecutionDto dto = new ExecutionDto();
        dto.setResultStatus(ResultStatus.FAILED);

        mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/execute")
                        .header("Authorization", "Bearer " + unassignedTesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRecordExecution_ClosedRun_Returns409() throws Exception {
        ExecutionDto dto = new ExecutionDto();
        dto.setResultStatus(ResultStatus.PASSED);

        mockMvc.perform(post("/api/runs/" + closedRun.getId() + "/cases/" + testCase.getId() + "/execute")
                        .header("Authorization", "Bearer " + assignedTesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void testReviewResult_LeaderApprove_Success() throws Exception {
        ExecutionDto dto = new ExecutionDto();
        dto.setResultStatus(ResultStatus.PASSED);
        mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/execute")
                        .header("Authorization", "Bearer " + assignedTesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/review")
                        .header("Authorization", "Bearer " + leaderToken)
                        .param("reviewed", "true")
                        .param("comment", "Looks good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isReviewed").value(true))
                .andExpect(jsonPath("$.data.reviewedByName").value("Leader Exec"));
    }

    @Test
    void testReviewResult_RequestRetestWithoutComment_Returns400() throws Exception {
        ExecutionDto dto = new ExecutionDto();
        dto.setResultStatus(ResultStatus.FAILED);
        mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/execute")
                        .header("Authorization", "Bearer " + assignedTesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/review")
                        .header("Authorization", "Bearer " + leaderToken)
                        .param("reviewed", "false"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Comment is required")));
    }

    @Test
    void testReviewResult_RequestRetestValid_Success() throws Exception {
        ExecutionDto dto = new ExecutionDto();
        dto.setResultStatus(ResultStatus.FAILED);
        mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/execute")
                        .header("Authorization", "Bearer " + assignedTesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/review")
                        .header("Authorization", "Bearer " + leaderToken)
                        .param("reviewed", "false")
                        .param("comment", "Please retest after fix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultStatus").value("RETEST"))
                .andExpect(jsonPath("$.data.isReviewed").value(false));
    }

    @Test
    void testReviewResult_TesterRole_Returns403() throws Exception {
        mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/review")
                        .header("Authorization", "Bearer " + assignedTesterToken)
                        .param("reviewed", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testReviewResult_UntestedCase_Returns400() throws Exception {
        mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/review")
                        .header("Authorization", "Bearer " + leaderToken)
                        .param("reviewed", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not been executed yet")));
    }

    @Test
    void testAttachmentUpload_And_SecurityFileAccess_Scenarios() throws Exception {
        // 1. Execute to get executionHistoryId
        ExecutionDto dto = new ExecutionDto();
        dto.setResultStatus(ResultStatus.PASSED);
        MvcResult execResult = mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/execute")
                        .header("Authorization", "Bearer " + assignedTesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        String execResponseBody = execResult.getResponse().getContentAsString();
        Long historyId = objectMapper.readTree(execResponseBody).get("data").get("latestExecutionHistoryId").asLong();

        // Invalid file format (text/plain) -> 400 Bad Request
        MockMultipartFile textFile = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
        mockMvc.perform(multipart("/api/attachments/upload")
                        .file(textFile)
                        .param("entityType", "EXECUTION_HISTORY")
                        .param("entityId", String.valueOf(historyId))
                        .header("Authorization", "Bearer " + assignedTesterToken))
                .andExpect(status().isBadRequest());

        // Valid image upload -> 200 OK, returns AttachmentDto without filePath
        MockMultipartFile imageFile = new MockMultipartFile("file", "screenshot.png", "image/png", new byte[]{1, 2, 3});
        MvcResult uploadResult = mockMvc.perform(multipart("/api/attachments/upload")
                        .file(imageFile)
                        .param("entityType", "EXECUTION_HISTORY")
                        .param("entityId", String.valueOf(historyId))
                        .header("Authorization", "Bearer " + assignedTesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl", notNullValue()))
                .andExpect(jsonPath("$.data.filePath").doesNotExist())
                .andReturn();

        Long attachmentId = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 2. GET /api/executions/{historyId}/attachments as project member -> 200 OK
        mockMvc.perform(get("/api/executions/" + historyId + "/attachments")
                        .header("Authorization", "Bearer " + assignedTesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(attachmentId))
                .andExpect(jsonPath("$.data[0].filePath").doesNotExist());

        // 3. GET /api/attachments/{id}/file as Project Member -> 200 OK
        mockMvc.perform(get("/api/attachments/" + attachmentId + "/file")
                        .header("Authorization", "Bearer " + assignedTesterToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));

        // 4. GET /api/attachments/{id}/file as External Non-Member -> 403 Forbidden
        mockMvc.perform(get("/api/attachments/" + attachmentId + "/file")
                        .header("Authorization", "Bearer " + externalTesterToken))
                .andExpect(status().isForbidden());

        // 5. GET /api/attachments/{id}/file without Token -> 401 Unauthorized
        mockMvc.perform(get("/api/attachments/" + attachmentId + "/file"))
                .andExpect(status().isUnauthorized());

        // 6. Direct public request to /uploads/{path} -> blocked (401 / 403)
        mockMvc.perform(get("/uploads/EXECUTION_HISTORY/" + historyId + "/screenshot.png"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testIdorSecurityGuardsOnExecutionEndpoints() throws Exception {
        // Execute to create a history entry in Project
        ExecutionDto dto = new ExecutionDto();
        dto.setResultStatus(ResultStatus.PASSED);
        MvcResult execResult = mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/execute")
                        .header("Authorization", "Bearer " + assignedTesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        String execResponseBody = execResult.getResponse().getContentAsString();
        Long historyId = objectMapper.readTree(execResponseBody).get("data").get("latestExecutionHistoryId").asLong();

        // 1. External tester calling GET /api/executions/{historyId}/attachments -> 403 Forbidden
        mockMvc.perform(get("/api/executions/" + historyId + "/attachments")
                        .header("Authorization", "Bearer " + externalTesterToken))
                .andExpect(status().isForbidden());

        // 2. External tester calling GET /api/runs/{runId}/cases/{caseId}/history -> 403 Forbidden
        mockMvc.perform(get("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/history")
                        .header("Authorization", "Bearer " + externalTesterToken))
                .andExpect(status().isForbidden());

        // 3. Project member calling GET /api/runs/{runId}/cases/{caseId}/history -> 200 OK
        mockMvc.perform(get("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/history")
                        .header("Authorization", "Bearer " + assignedTesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 4. Leader calling GET /api/runs/{runId}/cases/{caseId}/history -> 200 OK
        mockMvc.perform(get("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/history")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 5. Leader calling GET /api/executions/{historyId}/attachments -> 200 OK
        mockMvc.perform(get("/api/executions/" + historyId + "/attachments")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk());
    }

    @Test
    void testRecordExecution_MultipleAttempts_AppendsHistoryWithAttachments() throws Exception {
        ExecutionDto dto1 = new ExecutionDto();
        dto1.setResultStatus(ResultStatus.FAILED);
        dto1.setComment("Attempt 1 failed");

        mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/execute")
                        .header("Authorization", "Bearer " + assignedTesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isOk());

        Thread.sleep(1100);

        ExecutionDto dto2 = new ExecutionDto();
        dto2.setResultStatus(ResultStatus.PASSED);
        dto2.setComment("Attempt 2 passed");

        mockMvc.perform(post("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/execute")
                        .header("Authorization", "Bearer " + assignedTesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/runs/" + openRun.getId() + "/cases/" + testCase.getId() + "/history")
                        .header("Authorization", "Bearer " + assignedTesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].resultStatus").value("PASSED"))
                .andExpect(jsonPath("$.data[1].resultStatus").value("FAILED"));
    }
}
