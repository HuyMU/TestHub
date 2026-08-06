package com.testhub.testflowlite.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO: Replace with real user fetching from UserRepository in future task
        if ("leader".equals(username)) {
            return new User("leader", "$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07Xd0GlM95n9cR8Wiy", Collections.emptyList());
        }
        throw new UsernameNotFoundException("User not found: " + username);
    }
}
