package com.testhub.testflowlite.user;

import com.testhub.testflowlite.audit.AuditLogService;
import com.testhub.testflowlite.common.DuplicateResourceException;
import com.testhub.testflowlite.common.InvalidCredentialsException;
import com.testhub.testflowlite.common.ResourceNotFoundException;
import com.testhub.testflowlite.common.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<UserDto> getAllTesters() {
        return userRepository.findByRole(Role.TESTER).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return mapToDto(user);
    }

    @Transactional
    public UserDto createTester(CreateUserRequest request, String currentUsername) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        User tester = new User();
        tester.setUsername(request.getUsername());
        tester.setEmail(request.getEmail());
        tester.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        tester.setFullName(request.getFullName());
        tester.setRole(Role.TESTER);
        tester.setIsActive(true);

        User saved = userRepository.save(tester);

        User currentUser = userRepository.findByUsernameOrEmail(currentUsername, currentUsername).orElse(null);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        auditLogService.logAction(currentUserId, "CREATE_TESTER", "USER", saved.getId(), "Created Tester: " + saved.getUsername());

        return mapToDto(saved);
    }

    @Transactional
    public UserDto updateTester(Long id, UpdateUserRequest request, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!Objects.equals(user.getEmail(), request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setIsActive(request.getIsActive());

        User updated = userRepository.save(user);

        User currentUser = userRepository.findByUsernameOrEmail(currentUsername, currentUsername).orElse(null);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        auditLogService.logAction(currentUserId, "UPDATE_TESTER", "USER", updated.getId(),
                "Updated Tester active=" + updated.getIsActive() + ", email=" + updated.getEmail());

        return mapToDto(updated);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.logAction(user.getId(), "CHANGE_PASSWORD", "USER", user.getId(), "Changed personal password");
    }

    public UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getIsActive()
        );
    }
}
