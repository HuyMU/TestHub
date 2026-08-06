package com.testhub.testflowlite.user;

import com.testhub.testflowlite.common.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${SEED_LEADER_USERNAME:leader}")
    private String leaderUsername;

    @Value("${SEED_LEADER_EMAIL:leader@testhub.com}")
    private String leaderEmail;

    @Value("${SEED_LEADER_PASSWORD:Leader@123456}")
    private String leaderPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByRole(Role.LEADER)) {
            log.info("No Leader user found in database. Initializing default Leader account: {}", leaderUsername);
            User leader = new User();
            leader.setUsername(leaderUsername);
            leader.setEmail(leaderEmail);
            leader.setPasswordHash(passwordEncoder.encode(leaderPassword));
            leader.setFullName("System Leader");
            leader.setRole(Role.LEADER);
            leader.setIsActive(true);

            userRepository.save(leader);
            log.info("Leader user initialized successfully.");
        } else {
            log.info("Leader account already exists. Skipping leader seeding.");
        }
    }
}
