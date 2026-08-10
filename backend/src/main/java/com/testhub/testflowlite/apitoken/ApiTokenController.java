package com.testhub.testflowlite.apitoken;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
@Tag(name = "API Tokens", description = "API Token management endpoints (Leader only)")
@PreAuthorize("hasRole('LEADER')")
public class ApiTokenController {

    private final ApiTokenService apiTokenService;

    @PostMapping
    @Operation(summary = "Generate a new API token", description = "Generates a new API token. Plaintext token is returned ONLY ONCE in this response.")
    public ApiResponse<ApiTokenCreatedDto> generateToken(@AuthenticationPrincipal UserDetails userDetails) {
        ApiTokenCreatedDto dto = apiTokenService.generateToken(userDetails.getUsername());
        return ApiResponse.success("API Token generated successfully. Please save the plaintext token now as it will not be shown again.", dto);
    }

    @GetMapping
    @Operation(summary = "List API tokens", description = "Returns metadata list of all API tokens. Plaintext tokens and hashes are never returned.")
    public ApiResponse<List<ApiTokenDto>> listTokens(@AuthenticationPrincipal UserDetails userDetails) {
        List<ApiTokenDto> tokens = apiTokenService.listTokens(userDetails.getUsername());
        return ApiResponse.success("API Tokens retrieved successfully", tokens);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke an API token", description = "Revokes an API token so it can no longer be used for automated ingestion.")
    public ApiResponse<Void> revokeToken(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        apiTokenService.revokeToken(id, userDetails.getUsername());
        return ApiResponse.success("API Token revoked successfully", null);
    }
}
