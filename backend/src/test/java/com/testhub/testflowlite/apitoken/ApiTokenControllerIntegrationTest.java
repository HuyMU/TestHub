package com.testhub.testflowlite.apitoken;

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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApiTokenControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testhub_token_test")
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
    private ApiTokenRepository apiTokenRepository;

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
        apiTokenRepository.deleteAll();
        userRepository.deleteAll();

        leader = new User();
        leader.setUsername("token_leader");
        leader.setEmail("token_leader@testhub.com");
        leader.setPasswordHash(passwordEncoder.encode("password"));
        leader.setFullName("Token Leader");
        leader.setRole(Role.LEADER);
        leader.setIsActive(true);
        leader = userRepository.save(leader);

        tester = new User();
        tester.setUsername("token_tester");
        tester.setEmail("token_tester@testhub.com");
        tester.setPasswordHash(passwordEncoder.encode("password"));
        tester.setFullName("Token Tester");
        tester.setRole(Role.TESTER);
        tester.setIsActive(true);
        tester = userRepository.save(tester);

        leaderToken = jwtTokenProvider.generateAccessToken(leader.getUsername(), leader.getRole().name());
        testerToken = jwtTokenProvider.generateAccessToken(tester.getUsername(), tester.getRole().name());
    }

    @Test
    void testGenerateToken_Leader_ReturnsPlaintextOnce() throws Exception {
        mockMvc.perform(post("/api/tokens")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.plainTextToken", startsWith("thk_")))
                .andExpect(jsonPath("$.data.createdAt", notNullValue()));
    }

    @Test
    void testListTokens_Leader_DoesNotContainPlaintextOrHash() throws Exception {
        // Generate a token
        mockMvc.perform(post("/api/tokens")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk());

        // List tokens
        mockMvc.perform(get("/api/tokens")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].createdByFullName").value("Token Leader"))
                .andExpect(jsonPath("$.data[0].plainTextToken").doesNotExist())
                .andExpect(jsonPath("$.data[0].tokenHash").doesNotExist());
    }

    @Test
    void testTokenEndpoints_TesterRole_Returns403() throws Exception {
        // Generate -> 403
        mockMvc.perform(post("/api/tokens")
                        .header("Authorization", "Bearer " + testerToken))
                .andExpect(status().isForbidden());

        // List -> 403
        mockMvc.perform(get("/api/tokens")
                        .header("Authorization", "Bearer " + testerToken))
                .andExpect(status().isForbidden());

        // Revoke -> 403
        mockMvc.perform(delete("/api/tokens/1")
                        .header("Authorization", "Bearer " + testerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRevokeToken_IdempotentSuccess() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tokens")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andReturn();

        Long tokenId = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Revoke first time -> 200
        mockMvc.perform(delete("/api/tokens/" + tokenId)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk());

        ApiToken token = apiTokenRepository.findById(tokenId).orElseThrow();
        assertNotNull(token.getRevokedAt());

        // Revoke second time -> 200 (idempotent)
        mockMvc.perform(delete("/api/tokens/" + tokenId)
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk());
    }
}
