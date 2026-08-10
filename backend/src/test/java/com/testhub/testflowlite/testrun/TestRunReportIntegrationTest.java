package com.testhub.testflowlite.testrun;

import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.milestone.Milestone;
import com.testhub.testflowlite.milestone.MilestoneRepository;
import com.testhub.testflowlite.project.Project;
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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TestRunReportIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testhub_report_test")
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
    private Project project;
    private TestRun openRun;
    private String leaderToken;

    @BeforeEach
    void setUp() {
        testRunCaseRepository.deleteAll();
        testRunRepository.deleteAll();
        milestoneRepository.deleteAll();
        testCaseRepository.deleteAll();
        sectionRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        leader = new User();
        leader.setUsername("report_leader");
        leader.setEmail("report_leader@testhub.com");
        leader.setPasswordHash(passwordEncoder.encode("password"));
        leader.setFullName("Report Leader");
        leader.setRole(Role.LEADER);
        leader.setIsActive(true);
        leader = userRepository.save(leader);

        project = new Project();
        project.setName("Report Project");
        project.setCreatedBy(leader);
        project = projectRepository.save(project);

        Section section = new Section();
        section.setName("Report Section");
        section.setProject(project);
        section = sectionRepository.save(section);

        TestCase tc1 = new TestCase();
        tc1.setTitle("Report Case 1");
        tc1.setSection(section);
        tc1.setSteps("1. Do X");
        tc1.setExpectedResult("1. X passed");
        tc1.setStatus(TestCaseStatus.READY);
        tc1.setCreatedBy(leader);
        tc1 = testCaseRepository.save(tc1);

        TestCase tc2 = new TestCase();
        tc2.setTitle("Report Case 2");
        tc2.setSection(section);
        tc2.setSteps("1. Do Y");
        tc2.setExpectedResult("1. Y passed");
        tc2.setStatus(TestCaseStatus.READY);
        tc2 = testCaseRepository.save(tc2);

        Milestone milestone = new Milestone();
        milestone.setName("Release 1.0");
        milestone.setProject(project);
        milestone.setCreatedBy(leader);
        milestone = milestoneRepository.save(milestone);

        openRun = new TestRun();
        openRun.setName("Regression Run");
        openRun.setProject(project);
        openRun.setMilestone(milestone);
        openRun.setStatus(RunStatus.OPEN);
        openRun.setCreatedBy(leader);
        openRun = testRunRepository.save(openRun);

        TestRunCase trc1 = new TestRunCase();
        trc1.setRun(openRun);
        trc1.setCaseId(tc1.getId());
        trc1.setTitle(tc1.getTitle());
        trc1.setSteps(tc1.getSteps());
        trc1.setExpectedResult(tc1.getExpectedResult());
        trc1.setResultStatus(ResultStatus.PASSED);
        trc1.setExecutedBy("Tester A");
        testRunCaseRepository.save(trc1);

        TestRunCase trc2 = new TestRunCase();
        trc2.setRun(openRun);
        trc2.setCaseId(tc2.getId());
        trc2.setTitle(tc2.getTitle());
        trc2.setSteps(tc2.getSteps());
        trc2.setExpectedResult(tc2.getExpectedResult());
        trc2.setResultStatus(ResultStatus.FAILED);
        trc2.setExecutedBy("Tester B");
        testRunCaseRepository.save(trc2);

        leaderToken = jwtTokenProvider.generateAccessToken(leader.getUsername(), leader.getRole().name());
    }

    @Test
    void testGetTestRunReport_Json_Success() throws Exception {
        mockMvc.perform(get("/api/runs/" + openRun.getId() + "/report")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runId").value(openRun.getId()))
                .andExpect(jsonPath("$.data.runName").value("Regression Run"))
                .andExpect(jsonPath("$.data.totalCases").value(2))
                .andExpect(jsonPath("$.data.passedCases").value(1))
                .andExpect(jsonPath("$.data.failedCases").value(1))
                .andExpect(jsonPath("$.data.passRatePercentage").value(50.0))
                .andExpect(jsonPath("$.data.completionPercentage").value(100.0))
                .andExpect(jsonPath("$.data.cases", hasSize(2)));
    }

    @Test
    void testExportTestRunReport_Excel_Success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/runs/" + openRun.getId() + "/report/export")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=TestRun_Report_" + openRun.getId() + ".xlsx"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertTrue(content.length > 0);
    }
}
