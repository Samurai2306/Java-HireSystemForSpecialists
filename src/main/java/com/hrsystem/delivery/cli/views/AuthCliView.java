package com.hrsystem.delivery.cli.views;

import com.hrsystem.delivery.cli.CliSessionContext;
import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.dto.request.UserRegistrationDto;
import com.hrsystem.exception.AuthenticationException;
import com.hrsystem.repository.CandidateProfileRepository;
import com.hrsystem.repository.EmployerProfileRepository;
import com.hrsystem.service.AuthService;
import org.springframework.stereotype.Component;

import java.io.PrintStream;

@Component
public class AuthCliView {

    private final AuthService authService;
    private final CliSessionContext sessionContext;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final CandidateCliView candidateCliView;
    private final EmployerCliView employerCliView;
    private final AdminCliView adminCliView;

    public AuthCliView(AuthService authService,
                       CliSessionContext sessionContext,
                       CandidateProfileRepository candidateProfileRepository,
                       EmployerProfileRepository employerProfileRepository,
                       CandidateCliView candidateCliView,
                       EmployerCliView employerCliView,
                       AdminCliView adminCliView) {
        this.authService = authService;
        this.sessionContext = sessionContext;
        this.candidateProfileRepository = candidateProfileRepository;
        this.employerProfileRepository = employerProfileRepository;
        this.candidateCliView = candidateCliView;
        this.employerCliView = employerCliView;
        this.adminCliView = adminCliView;
    }

    public boolean showStartMenu(InputValidator input, PrintStream out) {
        out.println();
        out.println("============================================================");
        out.println("              HR-SYSTEM: AGGREGATOR & RECRUITING            ");
        out.println("============================================================");
        out.println("[1] Войти в систему (Login)");
        out.println("[2] Регистрация соискателя (Candidate Sign Up)");
        out.println("[3] Регистрация работодателя (Employer Sign Up)");
        out.println("[4] Просмотр каталога вакансий (Гостевой режим)");
        out.println("[0] Выход из программы");
        out.println("------------------------------------------------------------");
        int choice = input.readIntInRange("Выберите действие > ", 0, 4);
        switch (choice) {
            case 1 -> login(input, out);
            case 2 -> registerCandidate(input, out);
            case 3 -> registerEmployer(input, out);
            case 4 -> candidateCliView.catalog(null, false, input, out);
            case 0 -> {
                return false;
            }
            default -> {
            }
        }
        return true;
    }

    public void routeAuthenticated(InputValidator input, PrintStream out) {
        UserEntity user = sessionContext.getCurrentUser();
        if (user == null) {
            return;
        }
        switch (user.getRole()) {
            case CANDIDATE -> candidateCliView.show(
                    sessionContext.requireCandidateId(), user.getId(), input, out);
            case EMPLOYER -> employerCliView.show(
                    sessionContext.requireEmployerId(), user.getId(), input, out);
            case ADMIN -> adminCliView.show(input, out);
            default -> out.println("[Ошибка] Неизвестная роль");
        }
        sessionContext.logout();
        out.println("[OK] Сессия завершена");
    }

    private void login(InputValidator input, PrintStream out) {
        String email = input.readEmail("Email > ");
        String password = input.readPassword("Пароль > ");
        try {
            UserEntity user = authService.login(email, password);
            attachSession(user);
            out.println("[OK] Добро пожаловать, " + user.getEmail() + " (" + user.getRole() + ")");
            routeAuthenticated(input, out);
        } catch (AuthenticationException ex) {
            out.println("[Ошибка] " + ex.getMessage());
        }
    }

    private void registerCandidate(InputValidator input, PrintStream out) {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setRole(UserRole.CANDIDATE);
        dto.setEmail(input.readEmail("Email > "));
        dto.setPassword(input.readPassword("Пароль > "));
        dto.setFullName(input.readRequiredString("ФИО > "));
        dto.setTargetTitle(input.readOptionalString("Желаемая должность > "));
        dto.setSkills(input.readOptionalString("Навыки / стек > "));
        dto.setPhone(input.readOptionalString("Телефон > "));
        dto.setTelegram(input.readOptionalString("Telegram > "));
        try {
            UserEntity user = authService.register(dto);
            out.println("[OK] Соискатель зарегистрирован: " + user.getEmail());
            attachSession(user);
            routeAuthenticated(input, out);
        } catch (AuthenticationException ex) {
            out.println("[Ошибка] " + ex.getMessage());
        }
    }

    private void registerEmployer(InputValidator input, PrintStream out) {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setRole(UserRole.EMPLOYER);
        dto.setEmail(input.readEmail("Email > "));
        dto.setPassword(input.readPassword("Пароль > "));
        dto.setCompanyName(input.readRequiredString("Название компании > "));
        dto.setContactPerson(input.readRequiredString("Контактное лицо > "));
        String site = input.readOptionalString("Сайт компании (https://...) > ");
        if (!site.isBlank() && !input.isUrl(site)) {
            out.println("[!] Сайт пропущен: некорректный URL");
            site = "";
        }
        dto.setWebsiteUrl(site.isBlank() ? null : site);
        dto.setCompanyDescription(input.readOptionalString("Описание компании > "));
        try {
            UserEntity user = authService.register(dto);
            out.println("[OK] Работодатель зарегистрирован: " + user.getEmail());
            attachSession(user);
            routeAuthenticated(input, out);
        } catch (AuthenticationException ex) {
            out.println("[Ошибка] " + ex.getMessage());
        }
    }

    private void attachSession(UserEntity user) {
        sessionContext.login(
                user,
                candidateProfileRepository.findByUserId(user.getId()).orElse(null),
                employerProfileRepository.findByUserId(user.getId()).orElse(null)
        );
    }
}
