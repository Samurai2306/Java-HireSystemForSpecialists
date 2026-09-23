package com.hrsystem.delivery.cli.views;

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
import com.hrsystem.exception.AccessDeniedException;
import com.hrsystem.exception.EntityNotFoundException;
import com.hrsystem.exception.InvalidStateTransitionException;
import com.hrsystem.service.ApplicationService;
import com.hrsystem.service.EmployerService;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.util.List;

@Component
public class EmployerCliView {

    private final EmployerService employerService;
    private final ApplicationService applicationService;
    private final ConsoleTableFormatter tables = new ConsoleTableFormatter();

    public EmployerCliView(EmployerService employerService, ApplicationService applicationService) {
        this.employerService = employerService;
        this.applicationService = applicationService;
    }

    public void show(Long employerProfileId, Long userId, InputValidator input, PrintStream out) {
        boolean inMenu = true;
        while (inMenu) {
            out.println();
            out.println("=== КАБИНЕТ РАБОТОДАТЕЛЯ ===");
            out.println("[1] Мои вакансии");
            out.println("[2] Опубликовать новую вакансию");
            out.println("[3] Воронка откликов");
            out.println("[4] Профиль компании");
            out.println("[0] Выход в главное меню (Logout)");
            out.println("------------------------------------------------------------");
            try {
                int choice = input.readIntInRange("Выберите действие > ", 0, 4);
                if (choice == 1) {
                    listVacancies(employerProfileId, userId, input, out);
                } else if (choice == 2) {
                    createVacancy(employerProfileId, input, out);
                } else if (choice == 3) {
                    funnel(employerProfileId, userId, input, out);
                } else if (choice == 4) {
                    profile(employerProfileId, input, out);
                } else if (choice == 0) {
                    inMenu = false;
                }
            } catch (RuntimeException ex) {
                out.println("[Ошибка] " + ex.getMessage());
            }
        }
    }

    private void listVacancies(Long employerProfileId, Long userId, InputValidator input, PrintStream out) {
        List<EmployerVacancyRowDto> rows = employerService.listMyVacancies(employerProfileId);
        out.println();
        out.println("=== МОИ ВАКАНСИИ ===");
        out.println(tables.formatEmployerVacancies(rows));
        out.println("[1] Редактировать вилку/требования  [2] В архив  [3] Отклики  [0] Назад");
        int action = input.readIntInRange("Выберите действие > ", 0, 3);
        if (action == 0 || rows.isEmpty()) {
            return;
        }
        long vacancyId = input.readLong("ID вакансии > ");
        if (action == 1) {
            editVacancy(employerProfileId, vacancyId, input, out);
        } else if (action == 2) {
            employerService.archiveVacancy(employerProfileId, vacancyId);
            out.println("[OK] Вакансия #" + vacancyId + " переведена в архив и скрыта из поиска");
        } else if (action == 3) {
            showFunnelForVacancy(employerProfileId, userId, vacancyId, input, out);
        }
    }

    private void editVacancy(Long employerProfileId, long vacancyId, InputValidator input, PrintStream out) {
        VacancyEntity current = employerService.getOwnedVacancy(employerProfileId, vacancyId);
        out.println("Текущая вилка: " + tables.formatSalary(current));
        Integer min = input.readOptionalInt("Новая зарплата ОТ (Enter — без изменений) > ");
        Integer max = input.readOptionalInt("Новая зарплата ДО (Enter — без изменений) > ");
        String stack = input.readOptionalString("Новые требования/стек (Enter — без изменений) > ");
        if (stack.isBlank()) {
            stack = null;
        }
        employerService.updateVacancy(employerProfileId, vacancyId, min, max, stack);
        out.println("[OK] Вакансия #" + vacancyId + " обновлена");
    }

    private void createVacancy(Long employerProfileId, InputValidator input, PrintStream out) {
        out.println();
        out.println("=== ПУБЛИКАЦИЯ ВАКАНСИИ ===");
        CreateVacancyDto dto = new CreateVacancyDto();
        dto.setTitle(input.readRequiredString("Должность > "));
        dto.setSalaryMin(input.readOptionalInt("Зарплата ОТ > "));
        dto.setSalaryMax(input.readOptionalInt("Зарплата ДО > "));
        dto.setCurrency(readCurrency(input, out));
        dto.setRequirementsStack(input.readOptionalString("Стек / требования > "));
        dto.setDescription(input.readRequiredString("Описание > "));
        dto.setEmploymentType(readEmployment(input));
        dto.setLocation(input.readOptionalString("Локация > "));
        VacancyEntity created = employerService.createVacancy(employerProfileId, dto);
        out.println("[OK] Вакансия #" + created.getId() + " опубликована (is_parsed=false, статус ACTIVE, источник MANUAL)");
    }

    private Currency readCurrency(InputValidator input, PrintStream out) {
        out.println("Валюта: [1] RUB  [2] USD  [3] EUR  [4] KZT");
        int choice = input.readIntInRange("Валюта > ", 1, 4);
        if (choice == 2) {
            return Currency.USD;
        }
        if (choice == 3) {
            return Currency.EUR;
        }
        if (choice == 4) {
            return Currency.KZT;
        }
        return Currency.RUB;
    }

    private EmploymentType readEmployment(InputValidator input) {
        int choice = input.readIntInRange("Занятость [1] Remote [2] Office [3] Hybrid [4] Flexible > ", 1, 4);
        if (choice == 2) {
            return EmploymentType.OFFICE;
        }
        if (choice == 3) {
            return EmploymentType.HYBRID;
        }
        if (choice == 4) {
            return EmploymentType.FLEXIBLE;
        }
        return EmploymentType.REMOTE;
    }

    private void funnel(Long employerProfileId, Long userId, InputValidator input, PrintStream out) {
        List<EmployerVacancyRowDto> rows = employerService.listMyVacancies(employerProfileId);
        out.println();
        out.println("=== ВЫБОР ВАКАНСИИ ДЛЯ ВОРОНКИ ===");
        out.println(tables.formatEmployerVacancies(rows));
        if (rows.isEmpty()) {
            return;
        }
        long vacancyId = input.readLong("ID вакансии > ");
        showFunnelForVacancy(employerProfileId, userId, vacancyId, input, out);
    }

    private void showFunnelForVacancy(Long employerProfileId, Long userId, long vacancyId,
                                      InputValidator input, PrintStream out) {
        VacancyEntity vacancy = employerService.getOwnedVacancy(employerProfileId, vacancyId);
        boolean inFunnel = true;
        while (inFunnel) {
            List<ApplicationEntity> applications = employerService.getApplicationsForVacancy(employerProfileId, vacancyId);
            out.println();
            out.println("=== ОТКЛИКИ НА ВАКАНСИЮ #" + vacancy.getId() + ": " + vacancy.getTitle() + " ===");
            out.println(tables.formatApplications(applications));
            out.println("[1] Взять в работу (REVIEWING)");
            out.println("[2] Выставить оффер (OFFER)");
            out.println("[3] Отклонить (REJECTED)");
            out.println("[0] Назад");
            int action = input.readIntInRange("Выберите действие > ", 0, 3);
            if (action == 0) {
                inFunnel = false;
            } else if (applications.isEmpty()) {
                out.println("[!] Нет откликов");
            } else {
                long applicationId = input.readLong("ID отклика > ");
                String comment = input.readOptionalString("Комментарий работодателю (необязательно) > ");
                ApplicationStatus target = ApplicationStatus.REJECTED;
                if (action == 1) {
                    target = ApplicationStatus.REVIEWING;
                } else if (action == 2) {
                    target = ApplicationStatus.OFFER;
                }
                try {
                    ApplicationEntity updated = applicationService.changeStatus(
                            applicationId, target, comment, userId, UserRole.EMPLOYER);
                    out.println("[OK] Статус отклика #" + updated.getId() + " → " + updated.getStatus());
                } catch (InvalidStateTransitionException ex) {
                    out.println("[Ошибка] " + ex.getMessage());
                } catch (AccessDeniedException ex) {
                    out.println("[Ошибка] " + ex.getMessage());
                } catch (EntityNotFoundException ex) {
                    out.println("[Ошибка] " + ex.getMessage());
                }
            }
        }
    }

    private void profile(Long employerProfileId, InputValidator input, PrintStream out) {
        EmployerProfileEntity profile = employerService.getProfile(employerProfileId);
        out.println();
        out.println("=== ПРОФИЛЬ КОМПАНИИ ===");
        out.println("Компания: " + dash(profile.getCompanyName()));
        out.println("Контакт:  " + dash(profile.getContactPerson()));
        out.println("Сайт:     " + dash(profile.getWebsiteUrl()));
        out.println("Описание: " + dash(profile.getDescription()));
        if (!input.confirm("Изменить профиль?")) {
            return;
        }
        String company = input.readOptionalString("Название компании (Enter — без изменений) > ");
        String contact = input.readOptionalString("Контактное лицо (Enter — без изменений) > ");
        String site = input.readOptionalString("Сайт (Enter — без изменений) > ");
        String desc = input.readOptionalString("Описание (Enter — без изменений) > ");
        employerService.updateProfile(
                employerProfileId,
                blankToNull(company),
                blankToNull(contact),
                blankToNull(site),
                blankToNull(desc)
        );
        out.println("[OK] Профиль обновлён");
    }

    private static String dash(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return value;
    }

    private static String blankToNull(String value) {
        if (value.isBlank()) {
            return null;
        }
        return value;
    }
}
