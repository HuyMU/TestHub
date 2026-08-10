package com.testhub.testflowlite.apitoken;

import com.testhub.testflowlite.audit.AuditLogService;
import com.testhub.testflowlite.common.ForbiddenException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.common.Role;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiTokenService {

    private final ApiTokenRepository apiTokenRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public ApiTokenCreatedDto generateToken(String currentUsername) {
        User user = verifyLeader(currentUsername);

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String plainTextToken = "thk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = hashToken(plainTextToken);

        ApiToken token = new ApiToken();
        token.setCreatedBy(user);
        token.setTokenHash(tokenHash);
        token.setCreatedAt(LocalDateTime.now());
        token = apiTokenRepository.save(token);

        auditLogService.logAction(user.getId(), "CREATE_API_TOKEN", "API_TOKEN", token.getId(),
                "Generated API token #" + token.getId());

        return new ApiTokenCreatedDto(token.getId(), plainTextToken, token.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ApiTokenDto> listTokens(String currentUsername) {
        verifyLeader(currentUsername);
        List<ApiToken> tokens = apiTokenRepository.findAllByOrderByCreatedAtDesc();
        return tokens.stream().map(t -> new ApiTokenDto(
                t.getId(),
                t.getCreatedBy() != null ? t.getCreatedBy().getFullName() : "Unknown",
                t.getCreatedAt(),
                t.getLastUsedAt(),
                t.getRevokedAt()
        )).collect(Collectors.toList());
    }

    @Transactional
    public void revokeToken(Long tokenId, String currentUsername) {
        User user = verifyLeader(currentUsername);
        ApiToken token = apiTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("API token not found with id: " + tokenId));

        if (token.getRevokedAt() == null) {
            token.setRevokedAt(LocalDateTime.now());
            apiTokenRepository.save(token);
            auditLogService.logAction(user.getId(), "REVOKE_API_TOKEN", "API_TOKEN", token.getId(),
                    "Revoked API token #" + token.getId());
        }
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private User verifyLeader(String currentUsername) {
        User user = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));
        if (user.getRole() != Role.LEADER) {
            throw new ForbiddenException("Only Leaders can manage API tokens");
        }
        return user;
    }
}
