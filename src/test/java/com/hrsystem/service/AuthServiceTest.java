package com.hrsystem.service;

import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.dto.request.UserLoginDto;
import com.hrsystem.dto.request.UserRegistrationDto;
import com.hrsystem.exception.AuthenticationException;
import com.hrsystem.exception.ValidationException;
import com.hrsystem.repository.CandidateProfileRepository;
import com.hrsystem.repository.EmployerProfileRepository;
import com.hrsystem.repository.UserRepository;
import com.hrsystem.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private EmployerProfileRepository employerProfileRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, candidateProfileRepository, employerProfileRepository);
    }

    @Test
    @DisplayName("DoD-1: Регистрация соискателя сохраняет пароль исключительно в виде хэша BCrypt")
    void registerCandidate_shouldStoreBCryptHashAndCreateProfile() {
        UserRegistrationDto dto = new UserRegistrationDto(
                "damir@test.com",
                "secret123",
                "Дамир Галимов",
                "Senior Java Developer",
                "Java, Spring, PostgreSQL",
                "+79991112233",
                "@damir_dev",
                "https://github.com/damir"
        );

        when(userRepository.existsByEmail("damir@test.com")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        UserEntity user = authService.registerCandidate(dto);

        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("damir@test.com");
        assertThat(user.getRole()).isEqualTo(UserRole.CANDIDATE);

        // Verify password is BCrypt hash and NOT plain text
        assertThat(user.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(user.getPasswordHash()).startsWith("$2a$10$");
        assertThat(authService.verifyPassword("secret123", user.getPasswordHash())).isTrue();

        // Verify profile was saved
        ArgumentCaptor<CandidateProfileEntity> captor = ArgumentCaptor.forClass(CandidateProfileEntity.class);
        verify(candidateProfileRepository).save(captor.capture());
        CandidateProfileEntity savedProfile = captor.getValue();
        assertThat(savedProfile.getFullName()).isEqualTo("Дамир Галимов");
        assertThat(savedProfile.getTargetTitle()).isEqualTo("Senior Java Developer");
        assertThat(savedProfile.getSkills()).isEqualTo("Java, Spring, PostgreSQL");
        assertThat(savedProfile.getTelegram()).isEqualTo("@damir_dev");
    }

    @Test
    @DisplayName("Регистрация с уже существующим email отклоняется с ошибкой")
    void registerCandidate_shouldThrowWhenEmailAlreadyExists() {
        UserRegistrationDto dto = new UserRegistrationDto(
                "damir@test.com", "pass123", "Дамир", null, null, null, null, null
        );
        when(userRepository.existsByEmail("damir@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerCandidate(dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("уже зарегистрирован");
    }

    @Test
    @DisplayName("DoD-2: Вход с неверным паролем отклоняется с понятным сообщением")
    void login_withWrongPassword_shouldThrowAuthenticationException() {
        String hash = authService.hashPassword("correctPass");
        UserEntity existingUser = new UserEntity("damir@test.com", hash, UserRole.CANDIDATE);
        existingUser.setId(1L);

        when(userRepository.findByEmail("damir@test.com")).thenReturn(Optional.of(existingUser));

        UserLoginDto loginDto = new UserLoginDto("damir@test.com", "wrongPass");

        assertThatThrownBy(() -> authService.login(loginDto))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Неверный пароль");
    }

    @Test
    @DisplayName("DoD-2: Вход с несуществующим логином отклоняется с понятным сообщением")
    void login_withNonExistentEmail_shouldThrowAuthenticationException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        UserLoginDto loginDto = new UserLoginDto("unknown@test.com", "pass123");

        assertThatThrownBy(() -> authService.login(loginDto))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Пользователь с таким email не найден");
    }

    @Test
    @DisplayName("Успешный вход возвращает пользователя")
    void login_withValidCredentials_shouldReturnUser() {
        String hash = authService.hashPassword("validPass123");
        UserEntity existingUser = new UserEntity("damir@test.com", hash, UserRole.CANDIDATE);
        existingUser.setId(1L);
        existingUser.setIsActive(true);

        when(userRepository.findByEmail("damir@test.com")).thenReturn(Optional.of(existingUser));

        UserEntity loggedIn = authService.login(new UserLoginDto("damir@test.com", "validPass123"));

        assertThat(loggedIn).isNotNull();
        assertThat(loggedIn.getId()).isEqualTo(1L);
    }
}
