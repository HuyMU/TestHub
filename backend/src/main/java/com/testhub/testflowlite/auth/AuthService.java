package com.testhub.testflowlite.auth;

import com.testhub.testflowlite.common.ApiResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public ApiResponse<TokenResponse> login(LoginRequest request) {
        // TODO: Implement login logic
        TokenResponse response = new TokenResponse("mock-access-token", "mock-refresh-token", "leader");
        return ApiResponse.success("Login successful", response);
    }

    public ApiResponse<TokenResponse> refreshToken(String refreshToken) {
        // TODO: Implement token refresh logic
        TokenResponse response = new TokenResponse("mock-new-access-token", refreshToken, "leader");
        return ApiResponse.success("Token refreshed", response);
    }
}
