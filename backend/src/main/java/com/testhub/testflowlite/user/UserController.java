package com.testhub.testflowlite.user;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Tester account management (Leader only) & User Profile")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "List all Tester accounts (Leader only)")
    public ApiResponse<List<UserDto>> getAllTesters() {
        return ApiResponse.success(userService.getAllTesters());
    }

    @PostMapping
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Create a new Tester account (Leader only)")
    public ApiResponse<UserDto> createTester(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ApiResponse.success("Tester account created successfully", userService.createTester(request, currentUser.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Update Tester details or active status (Leader only)")
    public ApiResponse<UserDto> updateTester(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ApiResponse.success("Tester account updated successfully", userService.updateTester(id, request, currentUser.getUsername()));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get currently authenticated user details")
    public ApiResponse<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetails currentUser) {
        return ApiResponse.success(userService.getCurrentUser(currentUser.getUsername()));
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change personal password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        userService.changePassword(currentUser.getUsername(), request);
        return ApiResponse.success("Password changed successfully", null);
    }
}
