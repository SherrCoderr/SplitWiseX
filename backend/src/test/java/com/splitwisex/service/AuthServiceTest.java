package com.splitwisex.service;

import com.splitwisex.dto.auth.AuthResponse;
import com.splitwisex.dto.auth.LoginRequest;
import com.splitwisex.dto.auth.RegisterRequest;
import com.splitwisex.dto.auth.UserSummaryDto;
import com.splitwisex.entity.User;
import com.splitwisex.exception.DuplicateEmailException;
import com.splitwisex.exception.InvalidCredentialsException;
import com.splitwisex.mapper.UserMapper;
import com.splitwisex.repository.UserRepository;
import com.splitwisex.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // UserMapper has no dependencies, so a real instance is simpler than mocking it.
        authService = new AuthService(userRepository, passwordEncoder, jwtService, new UserMapper());
    }

    @Test
    void registerCreatesAUserWithAHashedPasswordAndReturnsAToken() {
        RegisterRequest request = new RegisterRequest("Sameer Kumar", "Sameer@Example.com", "password123");

        when(userRepository.existsByEmail("sameer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setId(1L);
            return toSave;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86_400_000L);

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("sameer@example.com"); // normalized to lowercase
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password"); // never the raw password
        assertThat(response.token()).isEqualTo("fake-jwt-token");
        assertThat(response.user()).isEqualTo(new UserSummaryDto(1L, "Sameer Kumar", "sameer@example.com"));
    }

    @Test
    void registerRejectsAnEmailThatAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Sameer", "sameer@example.com", "password123");
        when(userRepository.existsByEmail("sameer@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        User existingUser = User.builder()
                .id(7L).name("Rahul").email("rahul@example.com").passwordHash("hashed").build();

        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);
        when(jwtService.generateToken(existingUser)).thenReturn("fake-jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86_400_000L);

        AuthResponse response = authService.login(new LoginRequest("rahul@example.com", "correct-password"));

        assertThat(response.token()).isEqualTo("fake-jwt-token");
        assertThat(response.user().email()).isEqualTo("rahul@example.com");
    }

    @Test
    void loginFailsForAnUnknownEmailWithoutRevealingThat() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "whatever123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginFailsForAWrongPassword() {
        User existingUser = User.builder()
                .id(7L).name("Rahul").email("rahul@example.com").passwordHash("hashed").build();

        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("rahul@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
