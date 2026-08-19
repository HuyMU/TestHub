package com.testhub.testflowlite.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.security.JwtTokenProvider;
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

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProjectControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testhub_db_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mysql.getJdbcUrl() + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User leader;
    private User tester;
    private String leaderToken;
    private String testerToken;

    @BeforeEach
    void setUp() {
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
        tester.setUsername("tester1");
        tester.setEmail("tester1@testhub.com");
        tester.setPasswordHash(passwordEncoder.encode("Tester@123456"));
        tester.setFullName("Tester One");
        tester.setRole(Role.TESTER);
        tester.setIsActive(true);
        tester = userRepository.save(tester);

        leaderToken = jwtTokenProvider.generateAccessToken("leader", "LEADER");
        testerToken = jwtTokenProvider.generateAccessToken("tester1", "TESTER");
    }

    @Test
    void testCreateProject_LeaderSuccess_TesterForbidden() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("Project Alpha", "Primary testing project");

        // Leader creates -> 200 OK
        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Project Alpha"));

        // Tester creates -> 403 Forbidden
        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + testerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetProjects_RoleBasedVisibility() throws Exception {
        Project p1 = new Project();
        p1.setName("Project 1");
        p1.setCreatedBy(leader);
        p1 = projectRepository.save(p1);

        Project p2 = new Project();
        p2.setName("Project 2");
        p2.setCreatedBy(leader);
        p2 = projectRepository.save(p2);

        // Assign tester to Project 1 only
        ProjectMember pm = new ProjectMember();
        pm.setProject(p1);
        pm.setUser(tester);
        projectMemberRepository.save(pm);

        // Leader sees both projects (2)
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // Tester sees only assigned project (1)
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + testerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Project 1"));
    }

    @Test
    void testAssignMembers_Idempotent_AndLeaderRejection() throws Exception {
        Project p1 = new Project();
        p1.setName("Project 1");
        p1.setCreatedBy(leader);
        p1 = projectRepository.save(p1);

        AssignMembersRequest validRequest = new AssignMembersRequest(List.of(tester.getId()));

        // First assignment -> 200 OK
        mockMvc.perform(post("/api/projects/" + p1.getId() + "/members")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Second assignment (duplicate) -> 200 OK (Idempotent)
        mockMvc.perform(post("/api/projects/" + p1.getId() + "/members")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        // Attempting to assign LEADER user -> 400 Bad Request
        AssignMembersRequest invalidRequest = new AssignMembersRequest(List.of(leader.getId()));
        mockMvc.perform(post("/api/projects/" + p1.getId() + "/members")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testArchiveProject_StillVisible() throws Exception {
        Project p1 = new Project();
        p1.setName("Project Active");
        p1.setCreatedBy(leader);
        p1 = projectRepository.save(p1);

        UpdateProjectRequest updateRequest = new UpdateProjectRequest("Project Archived", "Updated desc", "Archived");

        mockMvc.perform(put("/api/projects/" + p1.getId())
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Archived"));

        // Archived project remains in list
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("Archived"));
    }
}
