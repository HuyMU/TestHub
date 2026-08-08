package com.testhub.testflowlite.milestone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.project.Project;
import com.testhub.testflowlite.project.ProjectMember;
import com.testhub.testflowlite.project.ProjectMemberRepository;
import com.testhub.testflowlite.project.ProjectRepository;
import com.testhub.testflowlite.security.JwtTokenProvider;
import com.testhub.testflowlite.testrun.TestRun;
import com.testhub.testflowlite.testrun.TestRunRepository;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MilestoneControllerIntegrationTest {

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
    private MilestoneRepository milestoneRepository;

    @Autowired
    private TestRunRepository testRunRepository;

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
        testRunRepository.deleteAll();
        milestoneRepository.deleteAll();
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
        project.setName("Milestone Integration Project");
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
    void testCreateMilestone_LeaderSuccess_TesterForbidden() throws Exception {
        CreateMilestoneRequest req = new CreateMilestoneRequest("Sprint 1 Release", LocalDate.now().plusDays(14));

        // Tester creation -> 403 Forbidden
        mockMvc.perform(post("/api/projects/" + project.getId() + "/milestones")
                        .header("Authorization", "Bearer " + testerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // Leader creation -> 200 OK
        mockMvc.perform(post("/api/projects/" + project.getId() + "/milestones")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Sprint 1 Release"));

        assertEquals(1, milestoneRepository.count());
    }

    @Test
    void testDeleteMilestone_ReferencedByTestRun_ReturnsConflict409() throws Exception {
        Milestone m = new Milestone();
        m.setProject(project);
        m.setName("Locked Milestone");
        m.setCreatedBy(leader);
        m = milestoneRepository.save(m);

        TestRun tr = new TestRun();
        tr.setProject(project);
        tr.setMilestone(m);
        tr.setName("Test Run linking Milestone");
        tr.setCreatedBy(leader);
        testRunRepository.save(tr);

        mockMvc.perform(delete("/api/projects/" + project.getId() + "/milestones/" + m.getId())
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("referenced by existing Test Runs")));
    }
}
