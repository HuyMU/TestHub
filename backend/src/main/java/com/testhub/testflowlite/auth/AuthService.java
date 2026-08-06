package com.testhub.testflowlite.auth;

import com.testhub.testflowlite.common.AccountDisabledException;
import com.testhub.testflowlite.common.ApiResponse;
import com.testhub.testflowlite.common.InvalidCredentialsException;
import com.testhub.testflowlite.security.JwtTokenProvider;
import com.testhub.testflowlite.user.User;
import com.testhub.testflowlite.user.UserDto;
import com.testhub.testflowlite.user.UserRepository;
import com.testhub.testflowlite.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Transactional(readOnly = true)
    public ApiResponse<TokenResponse> login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
            );
        } catch (DisabledException e) {
            log.warn("Login attempt for disabled account: {}", request.getUsernameOrEmail());
            throw new AccountDisabledException("Account is disabled");
        } catch (AuthenticationException e) {
            log.warn("Login failed for identifier: {}", request.getUsernameOrEmail());
            throw new InvalidCredentialsException("Invalid credentials");
        }

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());
        UserDto userDto = userService.mapToDto(user);

        return ApiResponse.success("Login successful", new TokenResponse(accessToken, refreshToken, userDto));
    }

    @Transactional(readOnly = true)
    public ApiResponse<TokenResponse> refreshToken(RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(request.getRefreshToken());
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new InvalidCredentialsException("User not found for token"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AccountDisabledException("Account is disabled");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getRole().name());
        UserDto userDto = userService.mapToDto(user);

        return ApiResponse.success("Token refreshed successfully", new TokenResponse(newAccessToken, request.getRefreshToken(), userDto));
    }
}
