package com.testhub.testflowlite.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserDto> getAllUsers() {
        // TODO: Implement get users logic in business task
        return Collections.emptyList();
    }

    public UserDto createUser(UserDto dto) {
        // TODO: Implement create tester logic in business task
        return dto;
    }
}
