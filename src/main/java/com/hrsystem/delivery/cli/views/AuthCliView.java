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

/** Стартовое меню: вход, регистрация соискателя/работодателя, гостевой режим. */
@Component
public class AuthCliView extends AbstractCliView {

    private final AuthService authService;

    @Autowired
    public AuthCliView(AuthService authService, CliSessionContext sessionContext, InputValidator input) {
        this(authService, sessionContext, input, System.out);
    }

    public AuthCliView(AuthService authService, CliSessionContext sessionContext,
                       InputValidator input, PrintStream out) {
        super(sessionContext, input, out);
        this.authService = authService;
    }

    /** @return false, только если пользователь выбрал выход из программы. */
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
            out.println(AnsiColor.colorize(LINE, AnsiColor.GRAY));

            switch (in.readIntInRange("Выберите действие [0-4]: ", 0, 4)) {
                case 1:
                    if (handleLogin()) {
                        return true;
                    }
                    break;
                case 2:
                    if (handleCandidateRegistration()) {
                        return true;
                    }
                    break;
                case 3:
                    if (handleEmployerRegistration()) {
                        return true;
                    }
                    break;
                case 4:
                    sessionContext.logout();
                    info("Вход в режиме Гостя (только просмотр каталога).");
                    return true;
                case 0:
                default:
                    out.println(AnsiColor.colorize("До свидания!", AnsiColor.YELLOW));
                    return false;
            }
        }
    }

    public boolean handleLogin() {
        out.println("\n" + AnsiColor.colorize("--- Авторизация в системе ---", AnsiColor.BOLD));
        String email = in.readEmail("Email: ");
        String password = in.readRequiredString("Пароль: ");

        try {
            UserEntity user = authService.login(new UserLoginDto(email, password));
            activateSession(user);
            return true;
        } catch (AuthenticationException | ValidationException e) {
            err("Ошибка авторизации: " + e.getMessage());
            return false;
        }
    }

    public boolean handleCandidateRegistration() {
        out.println("\n" + AnsiColor.colorize("--- Регистрация соискателя (все поля обязательны) ---", AnsiColor.BOLD));
        String email = in.readEmail("1/8 Email: ");
        String password = in.readPassword("2/8 Пароль (мин. 4 символа): ");
        String fullName = in.readRequiredString("3/8 ФИО: ");
        String targetTitle = in.readRequiredString("4/8 Желаемая должность (например, Java Developer): ");
        String skills = in.readRequiredString("5/8 Стек технологий и навыки (через запятую): ");
        String phone = in.readPhone("6/8 Телефон (пример: +7 (999) 123-45-67): ");
        String telegram = in.readTelegram("7/8 Telegram (@username): ");
        String portfolio = in.readUrl("8/8 Ссылка на GitHub / портфолио: ");

        UserRegistrationDto dto = new UserRegistrationDto(
                email, password, fullName, targetTitle, skills, phone, telegram, portfolio
        );

        try {
            UserEntity registeredUser = authService.registerCandidate(dto);
            sessionContext.setCandidateSession(registeredUser, authService.getCandidateProfile(registeredUser));
            ok("Регистрация соискателя успешно завершена!");
            return true;
        } catch (ValidationException e) {
            err("Ошибка при регистрации: " + e.getMessage());
            return false;
        }
    }

    public boolean handleEmployerRegistration() {
        out.println("\n" + AnsiColor.colorize("--- Регистрация работодателя (все поля обязательны) ---", AnsiColor.BOLD));
        String email = in.readEmail("1/5 Рабочий Email: ");
        String password = in.readPassword("2/5 Пароль (мин. 4 символа): ");
        String companyName = in.readRequiredString("3/5 Название компании: ");
        String contactPerson = in.readRequiredString("4/5 Контактное лицо (ФИО HR): ");
        String websiteUrl = in.readUrl("5/5 Сайт компании (http:// или https://): ");

        try {
            UserEntity registeredUser = authService.registerEmployer(email, password, companyName, contactPerson, websiteUrl);
            sessionContext.setEmployerSession(registeredUser, authService.getEmployerProfile(registeredUser));
            ok("Регистрация работодателя успешно завершена!");
            return true;
        } catch (ValidationException e) {
            err("Ошибка при регистрации: " + e.getMessage());
            return false;
        }
    }

    private void activateSession(UserEntity user) {
        switch (user.getRole()) {
            case CANDIDATE -> {
                CandidateProfileEntity profile = authService.getCandidateProfile(user);
                sessionContext.setCandidateSession(user, profile);
                String name = profile != null ? profile.getFullName() : user.getEmail();
                ok("Успешный вход! Добро пожаловать, " + name + "!");
            }
            case EMPLOYER -> {
                EmployerProfileEntity profile = authService.getEmployerProfile(user);
                sessionContext.setEmployerSession(user, profile);
                String company = profile != null ? profile.getCompanyName() : user.getEmail();
                ok("Успешный вход! Компания: " + company);
            }
            case ADMIN -> {
                sessionContext.setAdminSession(user);
                ok("Успешный вход с правами Администратора!");
            }
            default -> sessionContext.logout();
        }
    }
}
