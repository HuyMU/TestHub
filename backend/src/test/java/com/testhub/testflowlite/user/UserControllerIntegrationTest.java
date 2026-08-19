package com.testhub.testflowlite.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.security.JwtTokenProvider;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User leader;
    private String leaderToken;
    private String testerToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        leader = new User();
        leader.setUsername("leader");
        leader.setEmail("leader@testhub.com");
        leader.setPasswordHash(passwordEncoder.encode("Leader@123456"));
        leader.setFullName("System Leader");
        leader.setRole(Role.LEADER);
        leader.setIsActive(true);
        leader = userRepository.save(leader);

        User tester = new User();
        tester.setUsername("tester1");
        tester.setEmail("tester1@testhub.com");
        tester.setPasswordHash(passwordEncoder.encode("Tester@123456"));
        tester.setFullName("Tester One");
        tester.setRole(Role.TESTER);
        tester.setIsActive(true);
        userRepository.save(tester);

        leaderToken = jwtTokenProvider.generateAccessToken("leader", "LEADER");
        testerToken = jwtTokenProvider.generateAccessToken("tester1", "TESTER");
    }

    @Test
    void testCreateTester_ByLeader_Success() throws Exception {
        CreateUserRequest request = new CreateUserRequest("tester2", "tester2@testhub.com", "Pass@123456", "Tester Two");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("tester2"))
                .andExpect(jsonPath("$.data.role").value("TESTER"));
    }

    @Test
    void testCreateTester_PasswordTooShort_ReturnsBadRequest() throws Exception {
        CreateUserRequest request = new CreateUserRequest("tester_short", "short@testhub.com", "Short1A", "Short Password");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testCreateTester_PasswordNoUppercase_ReturnsBadRequest() throws Exception {
        CreateUserRequest request = new CreateUserRequest("tester_noupper", "noupper@testhub.com", "alllowercase1", "No Upper");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testCreateTester_PasswordNoDigit_ReturnsBadRequest() throws Exception {
        CreateUserRequest request = new CreateUserRequest("tester_nodigit", "nodigit@testhub.com", "NoDigitsHere", "No Digit");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testCreateTester_PasswordValid_Success() throws Exception {
        CreateUserRequest request = new CreateUserRequest("tester_valid", "valid@testhub.com", "ValidPass1", "Valid Pass Tester");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("tester_valid"))
                .andExpect(jsonPath("$.data.role").value("TESTER"));
    }

    @Test
    void testChangePassword_NewPasswordTooWeak_ReturnsBadRequest() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("Tester@123456", "weak");

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + testerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testCreateTester_ByTester_Forbidden() throws Exception {
        CreateUserRequest request = new CreateUserRequest("tester3", "tester3@testhub.com", "Pass@123456", "Tester Three");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + testerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateTester_LeaderTarget_NotFound() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("leader@testhub.com", "Hacked Leader", false);

        mockMvc.perform(put("/api/users/" + leader.getId())
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testChangePassword_Success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("Tester@123456", "NewPass@123456");

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + testerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
