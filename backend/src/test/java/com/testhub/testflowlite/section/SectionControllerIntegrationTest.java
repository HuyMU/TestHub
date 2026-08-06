package com.testhub.testflowlite.section;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.project.Project;
import com.testhub.testflowlite.project.ProjectMember;
import com.testhub.testflowlite.project.ProjectMemberRepository;
import com.testhub.testflowlite.project.ProjectRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SectionControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User leader;
    private User assignedTester;
    private User unassignedTester;
    private Project project;
    private String leaderToken;
    private String assignedTesterToken;
    private String unassignedTesterToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM test_cases");
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

        assignedTester = new User();
        assignedTester.setUsername("assigned_tester");
        assignedTester.setEmail("tester1@testhub.com");
        assignedTester.setPasswordHash(passwordEncoder.encode("Tester@123456"));
        assignedTester.setFullName("Assigned Tester");
        assignedTester.setRole(Role.TESTER);
        assignedTester.setIsActive(true);
        assignedTester = userRepository.save(assignedTester);

        unassignedTester = new User();
        unassignedTester.setUsername("unassigned_tester");
        unassignedTester.setEmail("tester2@testhub.com");
        unassignedTester.setPasswordHash(passwordEncoder.encode("Tester@123456"));
        unassignedTester.setFullName("Unassigned Tester");
        unassignedTester.setRole(Role.TESTER);
        unassignedTester.setIsActive(true);
        unassignedTester = userRepository.save(unassignedTester);

        project = new Project();
        project.setName("Integration Project");
        project.setCreatedBy(leader);
        project = projectRepository.save(project);

        ProjectMember pm = new ProjectMember();
        pm.setProject(project);
        pm.setUser(assignedTester);
        projectMemberRepository.save(pm);

        leaderToken = jwtTokenProvider.generateAccessToken("leader", "LEADER");
        assignedTesterToken = jwtTokenProvider.generateAccessToken("assigned_tester", "TESTER");
        unassignedTesterToken = jwtTokenProvider.generateAccessToken("unassigned_tester", "TESTER");
    }

    @Test
    void testCreateAndGetSectionTree_Success() throws Exception {
        CreateSectionRequest request = new CreateSectionRequest("Authentication", null, 0);

        mockMvc.perform(post("/api/projects/" + project.getId() + "/sections")
                        .header("Authorization", "Bearer " + assignedTesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Authentication"));

        mockMvc.perform(get("/api/projects/" + project.getId() + "/sections")
                        .header("Authorization", "Bearer " + assignedTesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Authentication"));
    }

    @Test
    void testUnassignedTester_AccessForbidden() throws Exception {
        mockMvc.perform(get("/api/projects/" + project.getId() + "/sections")
                        .header("Authorization", "Bearer " + unassignedTesterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteSection_NonEmptyWithChild_Conflict409() throws Exception {
        Section parent = new Section();
        parent.setProject(project);
        parent.setName("Parent Section");
        parent = sectionRepository.save(parent);

        Section child = new Section();
        child.setProject(project);
        child.setParentSection(parent);
        child.setName("Child Subsection");
        sectionRepository.save(child);

        // Leader tries deleting non-empty parent section -> 409 Conflict
        mockMvc.perform(delete("/api/sections/" + parent.getId())
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("child subsection(s)")));
    }

    @Test
    void testDeleteSection_NonEmptyWithTestCase_Conflict409() throws Exception {
        Section sec = new Section();
        sec.setProject(project);
        sec.setName("Login Module");
        sec = sectionRepository.save(sec);

        // Insert a dummy test case directly referencing sec.getId()
        jdbcTemplate.update("INSERT INTO test_cases (code, section_id, title, steps, expected_result, status, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)",
                "TC-0001", sec.getId(), "Test Login", "Steps", "Expected", "Draft", leader.getId());

        mockMvc.perform(delete("/api/sections/" + sec.getId())
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("test case(s)")));
    }

    @Test
    void testTesterDeleteSection_Forbidden403() throws Exception {
        Section sec = new Section();
        sec.setProject(project);
        sec.setName("Empty Section");
        sec = sectionRepository.save(sec);

        mockMvc.perform(delete("/api/sections/" + sec.getId())
                        .header("Authorization", "Bearer " + assignedTesterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testLeaderDeleteEmptySection_Success200() throws Exception {
        Section sec = new Section();
        sec.setProject(project);
        sec.setName("Empty Section");
        sec = sectionRepository.save(sec);

        mockMvc.perform(delete("/api/sections/" + sec.getId())
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testUpdateSection_CircularReference_BadRequest400() throws Exception {
        Section secA = new Section();
        secA.setProject(project);
        secA.setName("Section A");
        secA = sectionRepository.save(secA);

        Section secB = new Section();
        secB.setProject(project);
        secB.setParentSection(secA);
        secB.setName("Section B");
        secB = sectionRepository.save(secB);

        // Try setting secA's parent to secB (circular!)
        UpdateSectionRequest request = new UpdateSectionRequest("Section A", secB.getId(), 0);

        mockMvc.perform(put("/api/sections/" + secA.getId())
                        .header("Authorization", "Bearer " + assignedTesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Circular section reference detected")));
    }
}
