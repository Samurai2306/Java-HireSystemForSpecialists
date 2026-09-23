package com.hrsystem.delivery.cli.views;

import com.hrsystem.delivery.cli.CliSessionContext;
import com.hrsystem.delivery.cli.utils.ConsoleTableFormatter;
import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.domain.enums.Currency;
import com.hrsystem.domain.enums.EmploymentType;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.dto.request.CreateVacancyDto;
import com.hrsystem.dto.response.EmployerVacancyRowDto;
import com.hrsystem.service.ApplicationService;
import com.hrsystem.service.EmployerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.util.List;

/** Кабинет работодателя: вакансии, воронка откликов, профиль компании. */
@Component
public class EmployerCliView extends AbstractCliView implements RoleMenu {

    private static final String MENU = "[1] Мои вакансии  [2] Новая вакансия  [3] Воронка откликов  [4] Профиль  [0] Выход";

    private final EmployerService employerService;
    private final ApplicationService applicationService;
    private final ConsoleTableFormatter tables = new ConsoleTableFormatter();

    @Autowired
    public EmployerCliView(EmployerService employerService, ApplicationService applicationService,
                           CliSessionContext sessionContext, InputValidator input) {
        this(employerService, applicationService, sessionContext, input, System.out);
    }

    public EmployerCliView(EmployerService employerService, ApplicationService applicationService,
                           CliSessionContext sessionContext, InputValidator input, PrintStream out) {
        super(sessionContext, input, out);
        this.employerService = employerService;
        this.applicationService = applicationService;
    }

    @Override
    public boolean supports(UserRole role) {
        return role == UserRole.EMPLOYER;
    }

    @Override
    public void open() {
        Long employerId = sessionContext.requireEmployerId();
        Long userId = sessionContext.getCurrentUserId();
        while (true) {
            header("КАБИНЕТ РАБОТОДАТЕЛЯ");
            out.println(MENU);
            out.println(LINE);
            switch (in.readIntInRange("Выберите действие > ", 0, 4)) {
                case 1 -> safely(() -> listVacancies(employerId, userId));
                case 2 -> safely(() -> createVacancy(employerId));
                case 3 -> safely(() -> funnel(employerId, userId));
                case 4 -> safely(() -> profile(employerId));
                case 0 -> {
                    return;
                }
                default -> {
                }
            }
        }
    }

    private void listVacancies(Long employerId, Long userId) {
        List<EmployerVacancyRowDto> rows = employerService.listMyVacancies(employerId);
        out.println("\n=== МОИ ВАКАНСИИ ===");
        out.println(tables.formatEmployerVacancies(rows));
        out.println("[1] Редактировать вилку/требования  [2] В архив  [3] Отклики  [0] Назад");
        int action = in.readIntInRange("Выберите действие > ", 0, 3);
        if (action == 0 || rows.isEmpty()) {
            return;
        }
        long vacancyId = in.readLong("ID вакансии > ");
        switch (action) {
            case 1 -> editVacancy(employerId, vacancyId);
            case 2 -> {
                employerService.archiveVacancy(employerId, vacancyId);
                out.println("[OK] Вакансия #" + vacancyId + " переведена в архив и скрыта из поиска");
            }
            case 3 -> showFunnelForVacancy(employerId, userId, vacancyId);
            default -> {
            }
        }
    }

    private void editVacancy(Long employerId, long vacancyId) {
        VacancyEntity current = employerService.getOwnedVacancy(employerId, vacancyId);
        out.println("Текущая вилка: " + tables.formatSalary(current));
        Integer min = in.readOptionalInt("Новая зарплата ОТ (Enter — без изменений) > ");
        Integer max = in.readOptionalInt("Новая зарплата ДО (Enter — без изменений) > ");
        String stack = in.readOptionalString("Новые требования/стек (Enter — без изменений) > ");
        employerService.updateVacancy(employerId, vacancyId, min, max, stack.isBlank() ? null : stack);
        out.println("[OK] Вакансия #" + vacancyId + " обновлена");
    }

    private void createVacancy(Long employerId) {
        out.println("\n=== ПУБЛИКАЦИЯ ВАКАНСИИ ===");
        CreateVacancyDto dto = new CreateVacancyDto(
                in.readRequiredString("Должность > "),
                in.readOptionalInt("Зарплата ОТ > "),
                in.readOptionalInt("Зарплата ДО > "),
                readCurrency(),
                in.readOptionalString("Стек / требования > "),
                in.readRequiredString("Описание > "),
                readEmployment(),
                in.readOptionalString("Локация > ")
        );
        VacancyEntity created = employerService.createVacancy(employerId, dto);
        out.println("[OK] Вакансия #" + created.getId() + " опубликована (статус ACTIVE, источник MANUAL)");
    }

    private Currency readCurrency() {
        out.println("Валюта: [1] RUB  [2] USD  [3] EUR  [4] KZT");
        return switch (in.readIntInRange("Валюта > ", 1, 4)) {
            case 2 -> Currency.USD;
            case 3 -> Currency.EUR;
            case 4 -> Currency.KZT;
            default -> Currency.RUB;
        };
    }

    private EmploymentType readEmployment() {
        return switch (in.readIntInRange("Занятость [1] Remote [2] Office [3] Hybrid [4] Flexible > ", 1, 4)) {
            case 2 -> EmploymentType.OFFICE;
            case 3 -> EmploymentType.HYBRID;
            case 4 -> EmploymentType.FLEXIBLE;
            default -> EmploymentType.REMOTE;
        };
    }

    private void funnel(Long employerId, Long userId) {
        List<EmployerVacancyRowDto> rows = employerService.listMyVacancies(employerId);
        out.println("\n=== ВЫБОР ВАКАНСИИ ДЛЯ ВОРОНКИ ===");
        out.println(tables.formatEmployerVacancies(rows));
        if (!rows.isEmpty()) {
            showFunnelForVacancy(employerId, userId, in.readLong("ID вакансии > "));
        }
    }

    private void showFunnelForVacancy(Long employerId, Long userId, long vacancyId) {
        VacancyEntity vacancy = employerService.getOwnedVacancy(employerId, vacancyId);
        while (true) {
            List<ApplicationEntity> applications = employerService.getApplicationsForVacancy(employerId, vacancyId);
            out.println("\n=== ОТКЛИКИ НА ВАКАНСИЮ #" + vacancy.getId() + ": " + vacancy.getTitle() + " ===");
            out.println(tables.formatApplications(applications));
            out.println("[1] Взять в работу (REVIEWING)  [2] Оффер (OFFER)  [3] Отклонить (REJECTED)  [0] Назад");
            int action = in.readIntInRange("Выберите действие > ", 0, 3);
            if (action == 0) {
                return;
            }
            if (applications.isEmpty()) {
                warn("Нет откликов");
                continue;
            }
            long applicationId = in.readLong("ID отклика > ");
            String comment = in.readOptionalString("Комментарий (необязательно) > ");
            ApplicationStatus target = switch (action) {
                case 1 -> ApplicationStatus.REVIEWING;
                case 2 -> ApplicationStatus.OFFER;
                default -> ApplicationStatus.REJECTED;
            };
            safely(() -> {
                ApplicationEntity updated = applicationService.changeStatus(
                        applicationId, target, comment, userId, UserRole.EMPLOYER);
                out.println("[OK] Статус отклика #" + updated.getId() + " → " + updated.getStatus());
            });
        }
    }

    private void profile(Long employerId) {
        EmployerProfileEntity profile = employerService.getProfile(employerId);
        out.println("\n=== ПРОФИЛЬ КОМПАНИИ ===");
        out.println("Компания: " + dash(profile.getCompanyName()));
        out.println("Контакт:  " + dash(profile.getContactPerson()));
        out.println("Сайт:     " + dash(profile.getWebsiteUrl()));
        out.println("Описание: " + dash(profile.getDescription()));
        if (!in.confirm("Изменить профиль?")) {
            return;
        }
        employerService.updateProfile(employerId,
                blankToNull(in.readOptionalString("Название компании (Enter — без изменений) > ")),
                blankToNull(in.readOptionalString("Контактное лицо (Enter — без изменений) > ")),
                blankToNull(in.readOptionalUrl("Сайт (Enter — без изменений) > ", null)),
                blankToNull(in.readOptionalString("Описание (Enter — без изменений) > ")));
        out.println("[OK] Профиль обновлён");
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
