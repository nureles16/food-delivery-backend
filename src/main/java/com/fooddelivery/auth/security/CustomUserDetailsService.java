package com.fooddelivery.auth.security;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is disabled");
        }

        return new CustomUserDetails(user);
    }

    public UserDetails loadUserById(String userId) throws UsernameNotFoundException {
        UUID uuid;
        try {
            uuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("Invalid user ID format: " + userId);
        }

        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is disabled");
        }

        return new CustomUserDetails(user);
    }
}