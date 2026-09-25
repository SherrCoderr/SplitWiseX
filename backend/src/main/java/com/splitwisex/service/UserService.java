package com.splitwisex.service;

import com.splitwisex.entity.User;
import com.splitwisex.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Read-oriented user lookups. Kept separate from AuthService because
 * "find/check users" is a concern other services (group membership, expense
 * participants, etc.) will also need from Stage 3 onward, whereas
 * AuthService is specifically about the register/login flow.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }
}
