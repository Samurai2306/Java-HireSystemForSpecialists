package com.hrsystem.delivery.cli.views;

import com.hrsystem.delivery.cli.CliSessionContext;
import com.hrsystem.delivery.cli.utils.AnsiColor;
import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.dto.request.UserLoginDto;
import com.hrsystem.dto.request.UserRegistrationDto;
import com.hrsystem.exception.AuthenticationException;
import com.hrsystem.exception.ValidationException;
import com.hrsystem.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintStream;

@Component
public class AuthCliView {

    private final AuthService authService;
    private final CliSessionContext sessionContext;
    private final InputValidator inputValidator;
    private final PrintStream out;

    @Autowired
    public AuthCliView(AuthService authService, CliSessionContext sessionContext) {
        this(authService, sessionContext, new InputValidator(), System.out);
    }

    public AuthCliView(AuthService authService, CliSessionContext sessionContext,
                       InputValidator inputValidator, PrintStream out) {
        this.authService = authService;
        this.sessionContext = sessionContext;
        this.inputValidator = inputValidator;
        this.out = out;
    }

    public boolean showMainMenu() {
        while (true) {
            out.println("\n" + AnsiColor.colorize("================================================", AnsiColor.CYAN));
            out.println(AnsiColor.colorize("           HR-SYSTEM: СТАРТОВОЕ МЕНЮ           ", AnsiColor.BOLD + AnsiColor.CYAN));
            out.println(AnsiColor.colorize("================================================", AnsiColor.CYAN));
            out.println("  1. Вход в систему (Login)");
            out.println("  2. Регистрация соискателя (Candidate)");
            out.println("  3. Регистрация работодателя (Employer)");
            out.println("  4. Гостевой просмотр каталога (Guest)");
            out.println("  0. Выход из программы");
            out.println(AnsiColor.colorize("------------------------------------------------", AnsiColor.GRAY));

            int choice = inputValidator.readIntInRange("Выберите действие [0-4]: ", 0, 4);
            switch (choice) {
                case 1:
                    boolean loggedIn = handleLogin();
                    if (loggedIn) {
                        return true;
                    }
                    break;
                case 2:
                    handleCandidateRegistration();
                    break;
                case 3:
                    handleEmployerRegistration();
                    break;
                case 4:
                    sessionContext.logout();
                    out.println(AnsiColor.info("Вход в режиме Гостя (только просмотр каталога)."));
                    return true;
                case 0:
                    out.println(AnsiColor.colorize("До свидания!", AnsiColor.YELLOW));
                    return false;
            }
        }
    }

    public boolean handleLogin() {
        out.println("\n" + AnsiColor.colorize("--- Авторизация в системе ---", AnsiColor.BOLD));
        String email = inputValidator.readEmail("Email: ");
        String password = inputValidator.readNonEmptyString("Пароль: ");

        try {
            UserEntity user = authService.login(new UserLoginDto(email, password));
            switch (user.getRole()) {
                case CANDIDATE:
                    CandidateProfileEntity candidateProfile = authService.getCandidateProfile(user);
                    sessionContext.setCandidateSession(user, candidateProfile);
                    String candidateName = candidateProfile != null ? candidateProfile.getFullName() : user.getEmail();
                    out.println(AnsiColor.success("Успешный вход! Добро пожаловать, " + candidateName + "!"));
                    break;
                case EMPLOYER:
                    EmployerProfileEntity employerProfile = authService.getEmployerProfile(user);
                    sessionContext.setEmployerSession(user, employerProfile);
                    String compName = employerProfile != null ? employerProfile.getCompanyName() : user.getEmail();
                    out.println(AnsiColor.success("Успешный вход! Компания: " + compName));
                    break;
                case ADMIN:
                    sessionContext.setAdminSession(user);
                    out.println(AnsiColor.success("Успешный вход с правами Администратора!"));
                    break;
                default:
                    sessionContext.logout();
                    break;
            }
            return true;
        } catch (AuthenticationException | ValidationException e) {
            out.println(AnsiColor.error("Ошибка авторизации: " + e.getMessage()));
            return false;
        }
    }

    public void handleCandidateRegistration() {
        out.println("\n" + AnsiColor.colorize("--- Регистрация нового Соискателя ---", AnsiColor.BOLD));
        String email = inputValidator.readEmail("Введите Email: ");
        String password = inputValidator.readPassword("Придумайте пароль (мин. 4 символа): ");
        String fullName = inputValidator.readNonEmptyString("ФИО: ");
        String targetTitle = inputValidator.readOptionalString("Желаемая должность (например, Java Developer): ", null);
        String skills = inputValidator.readOptionalString("Стек технологий и навыки (через запятую): ", null);
        String phone = inputValidator.readOptionalString("Телефон: ", null);
        String telegram = inputValidator.readOptionalString("Telegram (@username): ", null);
        String portfolio = inputValidator.readOptionalString("Ссылки на GitHub / портфолио: ", null);

        UserRegistrationDto dto = new UserRegistrationDto(
                email, password, fullName, targetTitle, skills, phone, telegram, portfolio
        );

        try {
            UserEntity registeredUser = authService.registerCandidate(dto);
            CandidateProfileEntity profile = authService.getCandidateProfile(registeredUser);
            sessionContext.setCandidateSession(registeredUser, profile);
            out.println(AnsiColor.success("Регистрация соискателя успешно завершена! Сессия активирована."));
        } catch (ValidationException e) {
            out.println(AnsiColor.error("Ошибка при регистрации: " + e.getMessage()));
        }
    }

    public void handleEmployerRegistration() {
        out.println("\n" + AnsiColor.colorize("--- Регистрация нового Работодателя ---", AnsiColor.BOLD));
        String email = inputValidator.readEmail("Введите рабочий Email: ");
        String password = inputValidator.readPassword("Придумайте пароль (мин. 4 символа): ");
        String companyName = inputValidator.readNonEmptyString("Название компании: ");
        String contactPerson = inputValidator.readOptionalString("Контактное лицо (ФИО HR): ", null);
        String websiteUrl = inputValidator.readOptionalString("Сайт компании: ", null);

        try {
            UserEntity registeredUser = authService.registerEmployer(email, password, companyName, contactPerson, websiteUrl);
            EmployerProfileEntity profile = authService.getEmployerProfile(registeredUser);
            sessionContext.setEmployerSession(registeredUser, profile);
            out.println(AnsiColor.success("Регистрация работодателя успешно завершена! Сессия активирована."));
        } catch (ValidationException e) {
            out.println(AnsiColor.error("Ошибка при регистрации: " + e.getMessage()));
        }
    }
}
