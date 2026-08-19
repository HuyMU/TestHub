package com.testhub.testflowlite.auth;

import com.testhub.testflowlite.common.ApiResponse;
import com.testhub.testflowlite.common.InvalidCredentialsException;
import com.testhub.testflowlite.config.JwtConfig;
import com.testhub.testflowlite.security.RefreshCookieFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, Refresh Token, and Logout endpoints")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;
    private final JwtConfig jwtConfig;

    @PostMapping("/login")
    @Operation(summary = "User Login (Username or Email)")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        ApiResponse<TokenResponse> apiResponse = authService.login(request);
        if (apiResponse.getData() != null && apiResponse.getData().getRefreshToken() != null) {
            String refreshToken = apiResponse.getData().getRefreshToken();
            ResponseCookie cookie = refreshCookieFactory.buildRefreshCookie(
                    refreshToken,
                    jwtConfig.getRefreshExpirationMs() / 1000
            );
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            apiResponse.getData().setRefreshToken(null);
        }
        return apiResponse;
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token via HttpOnly Cookie")
    public ApiResponse<TokenResponse> refreshToken(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        ApiResponse<TokenResponse> apiResponse = authService.refreshToken(refreshToken);
        ResponseCookie cookie = refreshCookieFactory.buildRefreshCookie(
                refreshToken,
                jwtConfig.getRefreshExpirationMs() / 1000
        );
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        if (apiResponse.getData() != null) {
            apiResponse.getData().setRefreshToken(null);
        }
        return apiResponse;
    }

    @PostMapping("/logout")
    @Operation(summary = "User Logout (Clears Refresh Token Cookie)")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = refreshCookieFactory.buildRefreshCookie("", 0);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ApiResponse.success("Logged out successfully", null);
    }
}
