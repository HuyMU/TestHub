package com.testhub.testflowlite.dashboard;

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
import com.testhub.testflowlite.testrun.*;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DashboardControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testhub_dash_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
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

    private User leader;
    private User assignedTester;
    private User unassignedTester;
    private Project project;
    private String leaderToken;
    private String assignedTesterToken;
    private String unassignedTesterToken;

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
        leader.setUsername("dash_leader");
        leader.setEmail("dash_leader@testhub.com");
        leader.setPasswordHash(passwordEncoder.encode("password"));
        leader.setFullName("Dash Leader");
        leader.setRole(Role.LEADER);
        leader.setIsActive(true);
        leader = userRepository.save(leader);

        assignedTester = new User();
        assignedTester.setUsername("dash_assigned");
        assignedTester.setEmail("dash_assigned@testhub.com");
        assignedTester.setPasswordHash(passwordEncoder.encode("password"));
        assignedTester.setFullName("Assigned Tester");
        assignedTester.setRole(Role.TESTER);
        assignedTester.setIsActive(true);
        assignedTester = userRepository.save(assignedTester);

        unassignedTester = new User();
        unassignedTester.setUsername("dash_unassigned");
        unassignedTester.setEmail("dash_unassigned@testhub.com");
        unassignedTester.setPasswordHash(passwordEncoder.encode("password"));
        unassignedTester.setFullName("Unassigned Tester");
        unassignedTester.setRole(Role.TESTER);
        unassignedTester.setIsActive(true);
        unassignedTester = userRepository.save(unassignedTester);

        project = new Project();
        project.setName("Dashboard Project");
        project.setCreatedBy(leader);
        project = projectRepository.save(project);

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(assignedTester);
        projectMemberRepository.save(member);

        Section section = new Section();
        section.setName("Dash Section");
        section.setProject(project);
        section = sectionRepository.save(section);

        TestCase tc1 = new TestCase();
        tc1.setTitle("Ready Case 1");
        tc1.setSection(section);
        tc1.setSteps("Step 1");
        tc1.setExpectedResult("Pass");
        tc1.setStatus(TestCaseStatus.READY);
        tc1.setCreatedBy(leader);
        testCaseRepository.save(tc1);

        TestCase tc2 = new TestCase();
        tc2.setTitle("Review Case 2");
        tc2.setSection(section);
        tc2.setSteps("Step 1");
        tc2.setExpectedResult("Pass");
        tc2.setStatus(TestCaseStatus.REVIEW);
        tc2.setCreatedBy(assignedTester);
        testCaseRepository.save(tc2);

        Milestone milestone = new Milestone();
        milestone.setName("Sprint 1");
        milestone.setProject(project);
        milestone.setCreatedBy(leader);
        milestone = milestoneRepository.save(milestone);

        TestRun run = new TestRun();
        run.setName("Run 1");
        run.setProject(project);
        run.setMilestone(milestone);
        run.setStatus(RunStatus.OPEN);
        run.setCreatedBy(leader);
        run = testRunRepository.save(run);

        TestRunCase trc = new TestRunCase();
        trc.setRun(run);
        trc.setCaseId(tc1.getId());
        trc.setTitle(tc1.getTitle());
        trc.setSteps(tc1.getSteps());
        trc.setExpectedResult(tc1.getExpectedResult());
        trc.setResultStatus(ResultStatus.PASSED);
        testRunCaseRepository.save(trc);

        leaderToken = jwtTokenProvider.generateAccessToken(leader.getUsername(), leader.getRole().name());
        assignedTesterToken = jwtTokenProvider.generateAccessToken(assignedTester.getUsername(), assignedTester.getRole().name());
        unassignedTesterToken = jwtTokenProvider.generateAccessToken(unassignedTester.getUsername(), unassignedTester.getRole().name());
    }

    @Test
    void testDashboard_Leader_ReturnsAggregatedMetricsAndMilestoneProgress() throws Exception {
        mockMvc.perform(get("/api/dashboard/" + project.getId())
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCases").value(2))
                .andExpect(jsonPath("$.data.readyCases").value(1))
                .andExpect(jsonPath("$.data.reviewQueueCount").value(1))
                .andExpect(jsonPath("$.data.passedCount").value(1))
                .andExpect(jsonPath("$.data.milestoneProgress", hasSize(1)))
                .andExpect(jsonPath("$.data.milestoneProgress[0].milestoneName").value("Sprint 1"))
                .andExpect(jsonPath("$.data.milestoneProgress[0].totalRuns").value(1))
                .andExpect(jsonPath("$.data.milestoneProgress[0].completedCases").value(1))
                .andExpect(jsonPath("$.data.milestoneProgress[0].progressPercentage").value(100.0));
    }

    @Test
    void testDashboard_AssignedTester_ReturnsMetrics() throws Exception {
        mockMvc.perform(get("/api/dashboard/" + project.getId())
                        .header("Authorization", "Bearer " + assignedTesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCases").value(2));
    }

    @Test
    void testDashboard_UnassignedTester_Returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/" + project.getId())
                        .header("Authorization", "Bearer " + unassignedTesterToken))
                .andExpect(status().isForbidden());
    }
}
