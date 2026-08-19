package com.testhub.testflowlite.automation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testhub.testflowlite.apitoken.ApiToken;
import com.testhub.testflowlite.apitoken.ApiTokenRepository;
import com.testhub.testflowlite.apitoken.ApiTokenService;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.execution.ExecutionHistory;
import com.testhub.testflowlite.execution.ExecutionHistoryRepository;
import com.testhub.testflowlite.project.Project;
import com.testhub.testflowlite.project.ProjectRepository;
import com.testhub.testflowlite.section.Section;
import com.testhub.testflowlite.section.SectionRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AutomationControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testhub_auto_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mysql.getJdbcUrl() + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

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
    private ApiTokenRepository apiTokenRepository;

    @Autowired
    private ApiTokenService apiTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User leader;
    private Project project;
    private TestCase testCase;
    private TestRun openRun;
    private TestRun closedRun;
    private String plainTextToken;
    private ApiToken validToken;

    @BeforeEach
    void setUp() {
        executionHistoryRepository.deleteAll();
        testRunCaseRepository.deleteAll();
        testRunRepository.deleteAll();
        testCaseRepository.deleteAll();
        sectionRepository.deleteAll();
        projectRepository.deleteAll();
        apiTokenRepository.deleteAll();
        userRepository.deleteAll();

        leader = new User();
        leader.setUsername("auto_leader");
        leader.setEmail("auto_leader@testhub.com");
        leader.setPasswordHash(passwordEncoder.encode("password"));
        leader.setFullName("Auto Leader");
        leader.setRole(Role.LEADER);
        leader.setIsActive(true);
        leader = userRepository.save(leader);

        var createdDto = apiTokenService.generateToken(leader.getUsername());
        plainTextToken = createdDto.getPlainTextToken();
        validToken = apiTokenRepository.findById(createdDto.getId()).orElseThrow();

        project = new Project();
        project.setName("Automation Project");
        project.setCreatedBy(leader);
        project = projectRepository.save(project);

        Section section = new Section();
        section.setName("Auto Section");
        section.setProject(project);
        section = sectionRepository.save(section);

        testCase = new TestCase();
        testCase.setTitle("Automated Test Case");
        testCase.setSection(section);
        testCase.setSteps("1. Execute auto script");
        testCase.setExpectedResult("1. Pass");
        testCase.setStatus(TestCaseStatus.READY);
        testCase.setCreatedBy(leader);
        testCase = testCaseRepository.save(testCase);
        testCase.setCode(String.format("TC-%04d", testCase.getId()));
        testCase = testCaseRepository.save(testCase);

        openRun = new TestRun();
        openRun.setName("Open Auto Run");
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
        trc.setResultStatus(ResultStatus.UNTESTED);
        trc.setIsReviewed(false);
        testRunCaseRepository.save(trc);

        closedRun = new TestRun();
        closedRun.setName("Closed Auto Run");
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
        closedTrc.setResultStatus(ResultStatus.UNTESTED);
        closedTrc.setIsReviewed(false);
        testRunCaseRepository.save(closedTrc);
    }

    @Test
    void testSubmitResult_MissingOrInvalidToken_Returns401() throws Exception {
        AutomationResultDto dto = new AutomationResultDto(openRun.getId(), testCase.getCode(), "PASSED", 120L, "Pass", null);

        // Missing token header
        mockMvc.perform(post("/api/automation/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());

        // Garbage token
        mockMvc.perform(post("/api/automation/results")
                        .header("X-API-TOKEN", "invalid_token_123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testSubmitResult_RevokedToken_Returns401() throws Exception {
        apiTokenService.revokeToken(validToken.getId(), leader.getUsername());

        AutomationResultDto dto = new AutomationResultDto(openRun.getId(), testCase.getCode(), "PASSED", 120L, "Pass", null);

        mockMvc.perform(post("/api/automation/results")
                        .header("X-API-TOKEN", plainTextToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testSubmitResult_UnknownRunId_Returns404() throws Exception {
        AutomationResultDto dto = new AutomationResultDto(9999L, testCase.getCode(), "PASSED", 120L, "Pass", null);

        mockMvc.perform(post("/api/automation/results")
                        .header("X-API-TOKEN", plainTextToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSubmitResult_CaseRefNotInRun_Returns404() throws Exception {
        AutomationResultDto dto = new AutomationResultDto(openRun.getId(), "TC-9999", "PASSED", 120L, "Pass", null);

        mockMvc.perform(post("/api/automation/results")
                        .header("X-API-TOKEN", plainTextToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSubmitResult_ClosedRun_Returns409() throws Exception {
        AutomationResultDto dto = new AutomationResultDto(closedRun.getId(), testCase.getCode(), "PASSED", 120L, "Pass", null);

        mockMvc.perform(post("/api/automation/results")
                        .header("X-API-TOKEN", plainTextToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void testSubmitResult_InvalidStatusString_Returns400() throws Exception {
        AutomationResultDto dto = new AutomationResultDto(openRun.getId(), testCase.getCode(), "INVALID_STATUS", 120L, "Pass", null);

        mockMvc.perform(post("/api/automation/results")
                        .header("X-API-TOKEN", plainTextToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid result status")));
    }

    @Test
    void testSubmitResult_HappyPath_UpdatesCaseAndHistoryAndBumpsLastUsedAt() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        AutomationResultDto dto = new AutomationResultDto(openRun.getId(), testCase.getCode(), "PASSED", 1500L, "Automated test passed", now);

        mockMvc.perform(post("/api/automation/results")
                        .header("X-API-TOKEN", plainTextToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 1. Verify TestRunCase updated
        TestRunCase updatedCase = testRunCaseRepository.findByRunIdAndCaseId(openRun.getId(), testCase.getId()).orElseThrow();
        assertEquals(ResultStatus.PASSED, updatedCase.getResultStatus());
        assertEquals("Automated test passed", updatedCase.getComment());
        assertEquals("Automation (token #" + validToken.getId() + ")", updatedCase.getExecutedBy());
        assertFalse(updatedCase.getIsReviewed());

        // 2. Verify ExecutionHistory appended
        List<ExecutionHistory> histories = executionHistoryRepository.findByRunCaseIdOrderByExecutedAtDesc(updatedCase.getId());
        assertEquals(1, histories.size());
        assertEquals(ResultStatus.PASSED, histories.get(0).getResultStatus());
        assertEquals(1500L, histories.get(0).getDurationMs());
        assertEquals("Automation (token #" + validToken.getId() + ")", histories.get(0).getExecutedBy());

        // 3. Verify lastUsedAt updated on API token
        ApiToken reloadedToken = apiTokenRepository.findById(validToken.getId()).orElseThrow();
        assertNotNull(reloadedToken.getLastUsedAt());
    }
}
