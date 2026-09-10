package com.hrsystem.delivery.cli.views;

import com.hrsystem.delivery.cli.CliSessionContext;
import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.dto.request.UserLoginDto;
import com.hrsystem.dto.request.UserRegistrationDto;
import com.hrsystem.exception.AuthenticationException;
import com.hrsystem.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthCliViewTest {

    @Mock
    private AuthService authService;

    private CliSessionContext sessionContext;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setUp() {
        sessionContext = new CliSessionContext();
        outContent = new ByteArrayOutputStream();
    }

    @Test
    @DisplayName("Успешный вход соискателя в CLI активирует сессию")
    void handleLogin_successful() {
        String simulatedInput = "damir@test.com\nsecret123\n";
        InputValidator inputValidator = new InputValidator(
                new ByteArrayInputStream(simulatedInput.getBytes()),
                new PrintStream(outContent)
        );

        UserEntity user = new UserEntity("damir@test.com", "hash", UserRole.CANDIDATE);
        user.setId(1L);
        CandidateProfileEntity profile = new CandidateProfileEntity(user, "Дамир Галимов", "Java Dev", "Java", "123", "@tg");

        when(authService.login(any(UserLoginDto.class))).thenReturn(user);
        when(authService.getCandidateProfile(user)).thenReturn(profile);

        AuthCliView view = new AuthCliView(authService, sessionContext, inputValidator, new PrintStream(outContent));
        boolean result = view.handleLogin();

        assertThat(result).isTrue();
        assertThat(sessionContext.isCandidate()).isTrue();
        assertThat(sessionContext.getCurrentCandidateProfile().getFullName()).isEqualTo("Дамир Галимов");
        assertThat(outContent.toString()).contains("Добро пожаловать, Дамир Галимов");
    }

    @Test
    @DisplayName("Неверный пароль в CLI выводит ошибку и не создает сессию")
    void handleLogin_wrongPassword_shouldFail() {
        String simulatedInput = "damir@test.com\nwrongpass\n";
        InputValidator inputValidator = new InputValidator(
                new ByteArrayInputStream(simulatedInput.getBytes()),
                new PrintStream(outContent)
        );

        when(authService.login(any(UserLoginDto.class)))
                .thenThrow(new AuthenticationException("Неверный пароль."));

        AuthCliView view = new AuthCliView(authService, sessionContext, inputValidator, new PrintStream(outContent));
        boolean result = view.handleLogin();

        assertThat(result).isFalse();
        assertThat(sessionContext.isAuthorized()).isFalse();
        assertThat(outContent.toString()).contains("Ошибка авторизации: Неверный пароль.");
    }

    @Test
    @DisplayName("Регистрация нового соискателя через CLI")
    void handleCandidateRegistration_successful() {
        String simulatedInput = "new@candidate.com\npass1234\nИван Иванов\nJava Developer\nJava, Spring\n+79991234567\n@ivan\nhttps://github.com/ivan\n";
        InputValidator inputValidator = new InputValidator(
                new ByteArrayInputStream(simulatedInput.getBytes()),
                new PrintStream(outContent)
        );

        UserEntity user = new UserEntity("new@candidate.com", "hash", UserRole.CANDIDATE);
        user.setId(2L);
        CandidateProfileEntity profile = new CandidateProfileEntity(user, "Иван Иванов", "Java Developer", "Java, Spring", "+79991234567", "@ivan");

        when(authService.registerCandidate(any(UserRegistrationDto.class))).thenReturn(user);
        when(authService.getCandidateProfile(user)).thenReturn(profile);

        AuthCliView view = new AuthCliView(authService, sessionContext, inputValidator, new PrintStream(outContent));
        view.handleCandidateRegistration();

        assertThat(sessionContext.isCandidate()).isTrue();
        assertThat(outContent.toString()).contains("Регистрация соискателя успешно завершена!");
    }

    @Test
    @DisplayName("Выбор '0' в главном меню завершает программу")
    void showMainMenu_exit() {
        String simulatedInput = "0\n";
        InputValidator inputValidator = new InputValidator(
                new ByteArrayInputStream(simulatedInput.getBytes()),
                new PrintStream(outContent)
        );

        AuthCliView view = new AuthCliView(authService, sessionContext, inputValidator, new PrintStream(outContent));
        boolean proceed = view.showMainMenu();

        assertThat(proceed).isFalse();
        assertThat(outContent.toString()).contains("До свидания!");
    }
}
