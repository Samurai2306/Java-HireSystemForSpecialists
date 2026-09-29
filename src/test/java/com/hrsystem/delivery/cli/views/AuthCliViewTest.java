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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        boolean registered = view.handleCandidateRegistration();

        assertThat(registered).isTrue();
        assertThat(sessionContext.isCandidate()).isTrue();
        assertThat(outContent.toString()).contains("Регистрация соискателя успешно завершена!");
    }

    @Test
    @DisplayName("Регистрация: Enter не пропускает этапы — пустые поля отклоняются и переспрашиваются")
    void handleCandidateRegistration_doesNotSkipStepsWithEnter() {
        // Перед каждым обязательным ответом сначала пустая строка (Enter), затем корректное значение.
        String simulatedInput = ""
                + "\nnew@candidate.com\n"
                + "\npass1234\n"
                + "\nИван Иванов\n"
                + "\nJava Developer\n"
                + "\nJava, Spring\n"
                + "\n+79991234567\n"
                + "\n@ivan\n"
                + "\nhttps://github.com/ivan\n";
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
        boolean registered = view.handleCandidateRegistration();

        ArgumentCaptor<UserRegistrationDto> captor = ArgumentCaptor.forClass(UserRegistrationDto.class);
        verify(authService).registerCandidate(captor.capture());

        assertThat(registered).isTrue();
        assertThat(captor.getValue().getEmail()).isEqualTo("new@candidate.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("pass1234");
        assertThat(captor.getValue().getFullName()).isEqualTo("Иван Иванов");
        assertThat(captor.getValue().getTargetTitle()).isEqualTo("Java Developer");
        assertThat(captor.getValue().getSkills()).isEqualTo("Java, Spring");
        assertThat(captor.getValue().getPhone()).isEqualTo("+79991234567");
        assertThat(captor.getValue().getTelegram()).isEqualTo("@ivan");
        assertThat(captor.getValue().getPortfolioLinks()).isEqualTo("https://github.com/ivan");
        assertThat(outContent.toString()).contains("Поле не может быть пустым");
        assertThat(outContent.toString()).contains("Пароль должен содержать не менее 4 символов");
    }

    @Test
    @DisplayName("Регистрация работодателя: Enter не пропускает обязательные поля")
    void handleEmployerRegistration_doesNotSkipStepsWithEnter() {
        String simulatedInput = ""
                + "\nhr@company.com\n"
                + "\nemployer123\n"
                + "\nООО Ромашка\n"
                + "\nПетр Петров\n"
                + "\nhttps://romashka.ru\n";
        InputValidator inputValidator = new InputValidator(
                new ByteArrayInputStream(simulatedInput.getBytes()),
                new PrintStream(outContent)
        );

        UserEntity user = new UserEntity("hr@company.com", "hash", UserRole.EMPLOYER);
        user.setId(3L);

        when(authService.registerEmployer(eq("hr@company.com"), eq("employer123"), eq("ООО Ромашка"),
                eq("Петр Петров"), eq("https://romashka.ru"))).thenReturn(user);

        AuthCliView view = new AuthCliView(authService, sessionContext, inputValidator, new PrintStream(outContent));
        boolean registered = view.handleEmployerRegistration();

        assertThat(registered).isTrue();
        assertThat(sessionContext.isEmployer()).isTrue();
        assertThat(outContent.toString()).contains("Регистрация работодателя успешно завершена!");
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
