package com.testhub.testflowlite.user;

import com.testhub.testflowlite.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Tester account management (Leader only)")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "List all users")
    public ApiResponse<List<UserDto>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    @PostMapping
    @PreAuthorize("hasRole('LEADER')")
    @Operation(summary = "Create Tester account (Leader only)")
    public ApiResponse<UserDto> createUser(@RequestBody UserDto dto) {
        return ApiResponse.success("User created", userService.createUser(dto));
    }
}
