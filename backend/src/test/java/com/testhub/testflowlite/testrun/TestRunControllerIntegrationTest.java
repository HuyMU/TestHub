package com.testhub.testflowlite.testrun;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.milestone.Milestone;
import com.testhub.testflowlite.milestone.MilestoneRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TestRunControllerIntegrationTest {

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
    private MilestoneRepository milestoneRepository;

    @Autowired
    private TestRunRepository testRunRepository;

    @Autowired
    private TestRunCaseRepository testRunCaseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User leader;
    private User tester;
    private User unassignedUser;
    private Project project;
    private Section section;
    private TestCase readyCase;
    private TestCase draftCase;
    private String leaderToken;
    private String testerToken;

    @BeforeEach
    void setUp() {
        testRunCaseRepository.deleteAll();
        testRunRepository.deleteAll();
        milestoneRepository.deleteAll();
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

        unassignedUser = new User();
        unassignedUser.setUsername("other");
        unassignedUser.setEmail("other@testhub.com");
        unassignedUser.setPasswordHash(passwordEncoder.encode("Tester@123456"));
        unassignedUser.setFullName("Unassigned Tester");
        unassignedUser.setRole(Role.TESTER);
        unassignedUser.setIsActive(true);
        unassignedUser = userRepository.save(unassignedUser);

        project = new Project();
        project.setName("Test Run Integration Project");
        project.setCreatedBy(leader);
        project = projectRepository.save(project);

        ProjectMember pm = new ProjectMember();
        pm.setProject(project);
        pm.setUser(tester);
        projectMemberRepository.save(pm);

        section = new Section();
        section.setProject(project);
        section.setName("Core Section");
        section = sectionRepository.save(section);

        readyCase = new TestCase();
        readyCase.setSection(section);
        readyCase.setTitle("Original Ready Title");
        readyCase.setPrecondition("Precondition 1");
        readyCase.setSteps("1. Step 1");
        readyCase.setExpectedResult("1. Expected 1");
        readyCase.setTestData("Data 1");
        readyCase.setStatus(TestCaseStatus.READY);
        readyCase.setCreatedBy(leader);
        readyCase = testCaseRepository.save(readyCase);
        readyCase.setCode(String.format("TC-%04d", readyCase.getId()));
        readyCase = testCaseRepository.save(readyCase);

        draftCase = new TestCase();
        draftCase.setSection(section);
        draftCase.setTitle("Draft Case Title");
        draftCase.setPrecondition("Precondition 2");
        draftCase.setSteps("1. Step 1");
        draftCase.setExpectedResult("1. Expected 1");
        draftCase.setStatus(TestCaseStatus.DRAFT);
        draftCase.setCreatedBy(tester);
        draftCase = testCaseRepository.save(draftCase);
        draftCase.setCode(String.format("TC-%04d", draftCase.getId()));
        draftCase = testCaseRepository.save(draftCase);

        leaderToken = jwtTokenProvider.generateAccessToken("leader", "LEADER");
        testerToken = jwtTokenProvider.generateAccessToken("tester", "TESTER");
    }

    @Test
    void testCreateTestRun_OnlyReadyCasesByDefault_SuccessAndSnapshot() throws Exception {
        CreateTestRunRequest req = new CreateTestRunRequest();
        req.setName("Regression Run 1.0");
        req.setCases(List.of(new RunCaseItem(readyCase.getId(), tester.getId())));

        mockMvc.perform(post("/api/projects/" + project.getId() + "/runs")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Regression Run 1.0"))
                .andExpect(jsonPath("$.data.totalCases").value(1))
                .andExpect(jsonPath("$.data.cases[0].title").value("Original Ready Title"))
                .andExpect(jsonPath("$.data.cases[0].assignedToName").value("Assigned Tester"));

        List<TestRunCase> runCases = testRunCaseRepository.findAll();
        assertEquals(1, runCases.size());
        TestRunCase trc = runCases.get(0);
        assertEquals("Original Ready Title", trc.getTitle());
        assertEquals("Data 1", trc.getTestData());
    }

    @Test
    void testCreateTestRun_NonReadyCaseWithoutFlag_Returns400() throws Exception {
        CreateTestRunRequest req = new CreateTestRunRequest();
        req.setName("Run with Draft");
        req.setCases(List.of(new RunCaseItem(draftCase.getId(), tester.getId())));

        mockMvc.perform(post("/api/projects/" + project.getId() + "/runs")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not in READY status")));
    }

    @Test
    void testCreateTestRun_NonReadyCaseWithFlag_Success() throws Exception {
        CreateTestRunRequest req = new CreateTestRunRequest();
        req.setName("Emergency Run");
        req.setIncludeNonReady(true);
        req.setCases(List.of(new RunCaseItem(draftCase.getId(), tester.getId())));

        mockMvc.perform(post("/api/projects/" + project.getId() + "/runs")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCases").value(1));
    }

    @Test
    void testSnapshotImmutability_OriginalCaseModified_RunCaseUnchanged() throws Exception {
        CreateTestRunRequest req = new CreateTestRunRequest();
        req.setName("Snapshot Immutability Run");
        req.setCases(List.of(new RunCaseItem(readyCase.getId(), tester.getId())));

        mockMvc.perform(post("/api/projects/" + project.getId() + "/runs")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Modify original test case in test_cases table
        readyCase.setTitle("NEW MODIFIED TITLE AFTER RUN CREATION");
        readyCase.setSteps("New modified steps");
        testCaseRepository.save(readyCase);

        // Verify test_run_cases title is STILL "Original Ready Title" (Rule 11)
        List<TestRunCase> runCases = testRunCaseRepository.findAll();
        assertEquals(1, runCases.size());
        assertEquals("Original Ready Title", runCases.get(0).getTitle());
        assertEquals("1. Step 1", runCases.get(0).getSteps());
    }

    @Test
    void testAssignCaseToNonProjectMember_Returns400() throws Exception {
        CreateTestRunRequest req = new CreateTestRunRequest();
        req.setName("Invalid Assignment Run");
        req.setCases(List.of(new RunCaseItem(readyCase.getId(), unassignedUser.getId())));

        mockMvc.perform(post("/api/projects/" + project.getId() + "/runs")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not assigned to project")));
    }

    @Test
    void testCloseTestRun_CannotAddMoreCases_Returns409() throws Exception {
        CreateTestRunRequest req = new CreateTestRunRequest();
        req.setName("Run to Close");
        req.setCases(List.of(new RunCaseItem(readyCase.getId(), tester.getId())));

        MvcResult createResult = mockMvc.perform(post("/api/projects/" + project.getId() + "/runs")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Long runId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Close the run
        mockMvc.perform(post("/api/runs/" + runId + "/close")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closedAt").exists());

        // Attempting to add cases to closed run -> 409 Conflict
        AddCasesToRunRequest addReq = new AddCasesToRunRequest(List.of(new RunCaseItem(draftCase.getId(), tester.getId())));
        mockMvc.perform(post("/api/runs/" + runId + "/cases")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("closed Test Run")));
    }
}
