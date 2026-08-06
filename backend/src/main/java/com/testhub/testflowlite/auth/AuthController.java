package com.testhub.testflowlite.auth;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and Token Refresh endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User Login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest request) {
        // TODO: Implement authentication logic in business task
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token")
    public ApiResponse<TokenResponse> refreshToken(@RequestBody String refreshToken) {
        // TODO: Implement token refresh logic in business task
        return authService.refreshToken(refreshToken);
    }
}
