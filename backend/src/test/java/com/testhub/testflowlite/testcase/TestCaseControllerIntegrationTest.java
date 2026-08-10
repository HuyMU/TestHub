package com.testhub.testflowlite.testcase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.project.Project;
import com.testhub.testflowlite.project.ProjectMember;
import com.testhub.testflowlite.project.ProjectMemberRepository;
import com.testhub.testflowlite.project.ProjectRepository;
import com.testhub.testflowlite.section.Section;
import com.testhub.testflowlite.section.SectionRepository;
import com.testhub.testflowlite.security.JwtTokenProvider;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TestCaseControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User leader;
    private User testerA;
    private User testerB;
    private Project project;
    private Section section;
    private String leaderToken;
    private String testerAToken;
    private String testerBToken;

    @BeforeEach
    void setUp() {
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

        testerA = new User();
        testerA.setUsername("tester_a");
        testerA.setEmail("testera@testhub.com");
        testerA.setPasswordHash(passwordEncoder.encode("Tester@123456"));
        testerA.setFullName("Tester A");
        testerA.setRole(Role.TESTER);
        testerA.setIsActive(true);
        testerA = userRepository.save(testerA);

        testerB = new User();
        testerB.setUsername("tester_b");
        testerB.setEmail("testerb@testhub.com");
        testerB.setPasswordHash(passwordEncoder.encode("Tester@123456"));
        testerB.setFullName("Tester B");
        testerB.setRole(Role.TESTER);
        testerB.setIsActive(true);
        testerB = userRepository.save(testerB);

        project = new Project();
        project.setName("Test Case Project");
        project.setCreatedBy(leader);
        project = projectRepository.save(project);

        ProjectMember pmA = new ProjectMember();
        pmA.setProject(project);
        pmA.setUser(testerA);
        projectMemberRepository.save(pmA);

        ProjectMember pmB = new ProjectMember();
        pmB.setProject(project);
        pmB.setUser(testerB);
        projectMemberRepository.save(pmB);

        section = new Section();
        section.setProject(project);
        section.setName("Authentication Module");
        section = sectionRepository.save(section);

        leaderToken = jwtTokenProvider.generateAccessToken("leader", "LEADER");
        testerAToken = jwtTokenProvider.generateAccessToken("tester_a", "TESTER");
        testerBToken = jwtTokenProvider.generateAccessToken("tester_b", "TESTER");
    }

    @Test
    void testCreateTestCase_Success() throws Exception {
        CreateTestCaseRequest req = new CreateTestCaseRequest(
                section.getId(),
                "Verify Valid Login",
                "User is on login page",
                "1. Enter username\n2. Enter password\n3. Click Login",
                "Dashboard page is displayed",
                "valid_user / valid_pass",
                Priority.HIGH,
                TestCaseType.FUNCTIONAL,
                AutomationStatus.MANUAL
        );

        mockMvc.perform(post("/api/projects/" + project.getId() + "/cases")
                        .header("Authorization", "Bearer " + testerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code", startsWith("TC-")))
                .andExpect(jsonPath("$.data.title").value("Verify Valid Login"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.createdById").value(testerA.getId()));
    }

    @Test
    void testOwnerTesterUpdateDraftCase_Success() throws Exception {
        TestCase tc = createDummyTestCase(section, testerA, TestCaseStatus.DRAFT);

        UpdateTestCaseRequest req = new UpdateTestCaseRequest(
                section.getId(),
                "Updated Title by Owner",
                "New precondition",
                "New steps",
                "New expected result",
                "New test data",
                Priority.CRITICAL,
                TestCaseType.REGRESSION,
                AutomationStatus.AUTOMATED
        );

        mockMvc.perform(put("/api/cases/" + tc.getId())
                        .header("Authorization", "Bearer " + testerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Title by Owner"))
                .andExpect(jsonPath("$.data.priority").value("CRITICAL"));
    }

    @Test
    void testNonOwnerTesterUpdateCase_Forbidden403() throws Exception {
        TestCase tc = createDummyTestCase(section, testerA, TestCaseStatus.DRAFT);

        UpdateTestCaseRequest req = new UpdateTestCaseRequest(
                section.getId(),
                "Attempted Update by Non-Owner",
                "Precondition",
                "Steps",
                "Expected Result",
                "Test Data",
                Priority.HIGH,
                TestCaseType.FUNCTIONAL,
                AutomationStatus.MANUAL
        );

        mockMvc.perform(put("/api/cases/" + tc.getId())
                        .header("Authorization", "Bearer " + testerBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testOwnerTesterUpdateReviewCase_Conflict409() throws Exception {
        TestCase tc = createDummyTestCase(section, testerA, TestCaseStatus.REVIEW);

        UpdateTestCaseRequest req = new UpdateTestCaseRequest(
                section.getId(),
                "Attempted Update While Pending Review",
                "Precondition",
                "Steps",
                "Expected Result",
                "Test Data",
                Priority.HIGH,
                TestCaseType.FUNCTIONAL,
                AutomationStatus.MANUAL
        );

        mockMvc.perform(put("/api/cases/" + tc.getId())
                        .header("Authorization", "Bearer " + testerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("pending review")));
    }

    @Test
    void testOwnerTesterUpdateReadyCase_RevertsToDraft200() throws Exception {
        TestCase tc = createDummyTestCase(section, testerA, TestCaseStatus.READY);

        UpdateTestCaseRequest req = new UpdateTestCaseRequest(
                section.getId(),
                "Updated Ready Case by Tester",
                "Precondition",
                "Steps",
                "Expected Result",
                "Test Data",
                Priority.HIGH,
                TestCaseType.FUNCTIONAL,
                AutomationStatus.MANUAL
        );

        mockMvc.perform(put("/api/cases/" + tc.getId())
                        .header("Authorization", "Bearer " + testerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void testLeaderUpdateReadyCase_KeepsReady200() throws Exception {
        TestCase tc = createDummyTestCase(section, testerA, TestCaseStatus.READY);

        UpdateTestCaseRequest req = new UpdateTestCaseRequest(
                section.getId(),
                "Updated Ready Case by Leader",
                "Precondition",
                "Steps",
                "Expected Result",
                "Test Data",
                Priority.HIGH,
                TestCaseType.FUNCTIONAL,
                AutomationStatus.MANUAL
        );

        mockMvc.perform(put("/api/cases/" + tc.getId())
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));
    }

    @Test
    void testSubmitForReview_Success() throws Exception {
        TestCase tc = createDummyTestCase(section, testerA, TestCaseStatus.DRAFT);

        mockMvc.perform(post("/api/cases/" + tc.getId() + "/submit-review")
                        .header("Authorization", "Bearer " + testerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVIEW"))
                .andExpect(jsonPath("$.data.submittedAt", org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    void testSubmitForReview_LeaderForbidden403() throws Exception {
        TestCase tc = createDummyTestCase(section, testerA, TestCaseStatus.DRAFT);

        // Leader calling submit-review on Draft case -> 403 Forbidden
        mockMvc.perform(post("/api/cases/" + tc.getId() + "/submit-review")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testApproveTestCase_Success() throws Exception {
        TestCase tc = createDummyTestCase(section, testerA, TestCaseStatus.REVIEW);

        mockMvc.perform(post("/api/cases/" + tc.getId() + "/approve")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.reviewedById").value(leader.getId()));
    }

    @Test
    void testRejectTestCase_Success() throws Exception {
        TestCase tc = createDummyTestCase(section, testerA, TestCaseStatus.REVIEW);
        RejectTestCaseRequest req = new RejectTestCaseRequest("Please add more detailed steps.");

        mockMvc.perform(post("/api/cases/" + tc.getId() + "/reject")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.reviewComment").value("Please add more detailed steps."));
    }

    @Test
    void testTesterApproveOrReject_Forbidden403() throws Exception {
        TestCase tc = createDummyTestCase(section, testerA, TestCaseStatus.REVIEW);

        mockMvc.perform(post("/api/cases/" + tc.getId() + "/approve")
                        .header("Authorization", "Bearer " + testerAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteTestCase_OwnerDraftSuccess_NonOwnerForbidden() throws Exception {
        TestCase tc1 = createDummyTestCase(section, testerA, TestCaseStatus.DRAFT);

        // Tester B trying to delete Tester A's case -> 403
        mockMvc.perform(delete("/api/cases/" + tc1.getId())
                        .header("Authorization", "Bearer " + testerBToken))
                .andExpect(status().isForbidden());

        // Tester A deleting own case -> 200
        mockMvc.perform(delete("/api/cases/" + tc1.getId())
                        .header("Authorization", "Bearer " + testerAToken))
                .andExpect(status().isOk());
    }

    @Test
    void testCloneTestCase_Success() throws Exception {
        TestCase tc = createDummyTestCase(section, testerA, TestCaseStatus.READY);

        mockMvc.perform(post("/api/cases/" + tc.getId() + "/clone")
                        .header("Authorization", "Bearer " + testerBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(tc.getTitle() + " (Copy)"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.createdById").value(testerB.getId()));
    }

    @Test
    void testReviewQueue_LeaderSuccess_TesterForbidden() throws Exception {
        createDummyTestCase(section, testerA, TestCaseStatus.REVIEW);
        createDummyTestCase(section, testerB, TestCaseStatus.REVIEW);

        // Tester calling review-queue -> 403
        mockMvc.perform(get("/api/cases/review-queue")
                        .header("Authorization", "Bearer " + testerAToken))
                .andExpect(status().isForbidden());

        // Leader calling review-queue -> 200 with 2 items
        mockMvc.perform(get("/api/cases/review-queue")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void testReviewQueue_FifoOrderingBySubmittedAt() throws Exception {
        TestCase caseA = createDummyTestCase(section, testerA, TestCaseStatus.DRAFT);
        TestCase caseB = createDummyTestCase(section, testerB, TestCaseStatus.DRAFT);

        // Tester B submits case B for review FIRST
        mockMvc.perform(post("/api/cases/" + caseB.getId() + "/submit-review")
                        .header("Authorization", "Bearer " + testerBToken))
                .andExpect(status().isOk());

        // Tester A submits case A for review SECOND (even though case A was created before case B)
        mockMvc.perform(post("/api/cases/" + caseA.getId() + "/submit-review")
                        .header("Authorization", "Bearer " + testerAToken))
                .andExpect(status().isOk());

        // Review queue should return [caseB, caseA] sorted by submittedAt ASC
        mockMvc.perform(get("/api/cases/review-queue")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(caseB.getId()))
                .andExpect(jsonPath("$.data[1].id").value(caseA.getId()));
    }

    @Test
    void testFilterAndPagination_Success() throws Exception {
        createDummyTestCase(section, testerA, TestCaseStatus.DRAFT);
        createDummyTestCase(section, testerA, TestCaseStatus.READY);

        mockMvc.perform(get("/api/projects/" + project.getId() + "/cases?status=DRAFT&page=0&size=10")
                        .header("Authorization", "Bearer " + testerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].status").value("DRAFT"));
    }

    private TestCase createDummyTestCase(Section sec, User creator, TestCaseStatus status) {
        TestCase tc = new TestCase();
        tc.setSection(sec);
        tc.setTitle("Dummy Test Case");
        tc.setPrecondition("Precondition");
        tc.setSteps("1. Step one");
        tc.setExpectedResult("Expected result");
        tc.setTestData("Test data");
        tc.setPriority(Priority.MEDIUM);
        tc.setType(TestCaseType.FUNCTIONAL);
        tc.setAutomationStatus(AutomationStatus.MANUAL);
        tc.setStatus(status);
        tc.setCreatedBy(creator);
        tc = testCaseRepository.save(tc);

        tc.setCode(String.format("TC-%04d", tc.getId()));
        return testCaseRepository.save(tc);
    }
}
