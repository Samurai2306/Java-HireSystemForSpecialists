package com.hrsystem.delivery.cli.views;

import com.hrsystem.delivery.cli.utils.ConsoleTableFormatter;
import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.dto.request.VacancyFilterDto;
import com.hrsystem.exception.DuplicateApplicationException;
import com.hrsystem.service.ApplicationService;
import com.hrsystem.service.CandidateService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.util.List;

@Component
public class CandidateCliView {

    private final CandidateService candidateService;
    private final ApplicationService applicationService;
    private final ConsoleTableFormatter tables;

    public CandidateCliView(CandidateService candidateService,
                            ApplicationService applicationService,
                            ConsoleTableFormatter tables) {
        this.candidateService = candidateService;
        this.applicationService = applicationService;
        this.tables = tables;
    }

    public void show(Long candidateProfileId, Long userId, InputValidator input, PrintStream out) {
        boolean inMenu = true;
        while (inMenu) {
            out.println();
            out.println("=== КАБИНЕТ СОИСКАТЕЛЯ ===");
            out.println("[1] Каталог вакансий");
            out.println("[2] Мои отклики");
            out.println("[3] Мой профиль / резюме");
            out.println("[0] Выход в главное меню (Logout)");
            int choice = input.readIntInRange("Выберите действие > ", 0, 3);
            try {
                switch (choice) {
                    case 1 -> catalog(candidateProfileId, true, input, out);
                    case 2 -> myApplications(candidateProfileId, userId, input, out);
                    case 3 -> profile(candidateProfileId, input, out);
                    case 0 -> inMenu = false;
                    default -> {
                    }
                }
            } catch (RuntimeException ex) {
                out.println("[Ошибка] " + ex.getMessage());
            }
        }
    }

    public void catalog(Long candidateProfileId, boolean canApply, InputValidator input, PrintStream out) {
        VacancyFilterDto filter = new VacancyFilterDto();
        boolean browsing = true;
        while (browsing) {
            Page<VacancyEntity> page = candidateService.searchVacancies(filter);
            out.println();
            out.println("=== КАТАЛОГ ВАКАНСИЙ ===");
            out.println(tables.formatVacancies(page.getContent()));
            out.println("Страница " + (page.getNumber() + 1) + " из " + Math.max(page.getTotalPages(), 1)
                    + " (Всего найдено: " + page.getTotalElements() + ")");
            out.println("[N] Следующая | [P] Предыдущая | [F] Фильтры | [D] Детали"
                    + (canApply ? " | [O] Откликнуться" : "") + " | [B] Назад");
            String cmd = input.readOptionalString("Выберите действие > ").trim().toUpperCase();
            switch (cmd) {
                case "N" -> {
                    if (page.hasNext()) {
                        filter.setPage(filter.getPage() + 1);
                    } else {
                        out.println("[!] Это последняя страница");
                    }
                }
                case "P" -> {
                    if (page.hasPrevious()) {
                        filter.setPage(filter.getPage() - 1);
                    } else {
                        out.println("[!] Это первая страница");
                    }
                }
                case "F" -> {
                    applyFilters(filter, input, out);
                    filter.setPage(0);
                }
                case "D" -> showDetails(input, out);
                case "O" -> {
                    if (canApply) {
                        applyToVacancy(candidateProfileId, input, out);
                    }
                }
                case "B", "0", "" -> browsing = false;
                default -> out.println("[!] Неизвестная команда");
            }
        }
    }

    private void applyFilters(VacancyFilterDto filter, InputValidator input, PrintStream out) {
        String keyword = input.readOptionalString("Ключевое слово (Enter — сброс) > ");
        filter.setKeyword(keyword.isBlank() ? null : keyword);
        Integer salary = input.readOptionalInt("Минимальная зарплата (Enter — сброс) > ");
        filter.setSalaryMin(salary);
        out.println("Источник: [0] ALL [1] WEBSITE [2] TELEGRAM [3] MANUAL");
        int src = input.readIntInRange("Источник > ", 0, 3);
        filter.setSourceType(switch (src) {
            case 1 -> VacancySource.WEBSITE;
            case 2 -> VacancySource.TELEGRAM;
            case 3 -> VacancySource.MANUAL;
            default -> null;
        });
    }

    private void showDetails(InputValidator input, PrintStream out) {
        long id = input.readLong("ID вакансии > ");
        VacancyEntity vacancy = candidateService.getVacancy(id);
        out.println();
        out.println("=== ВАКАНСИЯ #" + vacancy.getId() + " ===");
        out.println("Должность:   " + vacancy.getTitle());
        out.println("Компания:    " + vacancy.getCompanyName());
        out.println("Зарплата:    " + tables.formatSalary(vacancy));
        out.println("Источник:    " + tables.sourceMarker(vacancy.getSourceType()));
        out.println("Занятость:   " + vacancy.getEmploymentType());
        out.println("Локация:     " + vacancy.getLocation());
        out.println("Статус:      " + vacancy.getStatus());
        out.println("Стек:        " + nullSafe(vacancy.getRequirementsStack()));
        out.println("Описание:");
        out.println(vacancy.getDescription());
        if (vacancy.getSourceUrl() != null) {
            out.println("Ссылка:      " + vacancy.getSourceUrl());
        }
    }

    private void applyToVacancy(Long candidateProfileId, InputValidator input, PrintStream out) {
        long vacancyId = input.readLong("ID вакансии для отклика > ");
        VacancyEntity vacancy = candidateService.getVacancy(vacancyId);
        out.println(">>> ПОДАЧА ОТКЛИКА НА ВАКАНСИЮ #" + vacancy.getId() + " (" + vacancy.getTitle() + " - "
                + vacancy.getCompanyName() + ")");
        String letter = input.readOptionalString("Сопроводительное письмо (Enter — из профиля) > ");
        int confirm = input.readIntInRange("[1] Подтвердить отправку  [2] Отмена > ", 1, 2);
        if (confirm != 1) {
            return;
        }
        try {
            ApplicationEntity saved = applicationService.apply(candidateProfileId, vacancyId, letter);
            out.println("[OK] Отклик успешно отправлен! Статус: " + saved.getStatus());
        } catch (DuplicateApplicationException | IllegalArgumentException ex) {
            out.println("[Ошибка] " + ex.getMessage());
        }
    }

    private void myApplications(Long candidateProfileId, Long userId, InputValidator input, PrintStream out) {
        boolean inApps = true;
        while (inApps) {
            List<ApplicationEntity> applications = candidateService.listMyApplications(candidateProfileId);
            out.println();
            out.println("=== МОИ ОТКЛИКИ ===");
            out.println(tables.formatCandidateApplications(applications));
            out.println("[1] Детали  [2] Отозвать  [0] Назад");
            int action = input.readIntInRange("Действие > ", 0, 2);
            if (action == 0) {
                inApps = false;
            } else if (applications.isEmpty()) {
                out.println("[!] Нет откликов");
            } else if (action == 1) {
                long id = input.readLong("ID отклика > ");
                ApplicationEntity app = candidateService.getMyApplication(candidateProfileId, id);
                out.println("Статус:     " + tables.colorStatus(app.getStatus()));
                out.println("Комментарий работодателя: " + nullSafe(app.getStatusComment()));
                out.println("Письмо:");
                out.println(nullSafe(app.getCoverLetter()));
            } else {
                long id = input.readLong("ID отклика для отзыва > ");
                ApplicationEntity withdrawn = applicationService.changeStatus(
                        id,
                        com.hrsystem.domain.enums.ApplicationStatus.WITHDRAWN,
                        "Отзыв соискателем",
                        userId,
                        UserRole.CANDIDATE
                );
                out.println("[OK] Отклик #" + withdrawn.getId() + " отозван (" + withdrawn.getStatus() + ")");
            }
        }
    }

    private void profile(Long candidateProfileId, InputValidator input, PrintStream out) {
        CandidateProfileEntity profile = candidateService.getProfile(candidateProfileId);
        out.println();
        out.println("=== ПРОФИЛЬ СОИСКАТЕЛЯ ===");
        out.println("ФИО:        " + profile.getFullName());
        out.println("Должность:  " + nullSafe(profile.getTargetTitle()));
        out.println("Навыки:     " + nullSafe(profile.getSkills()));
        out.println("Телефон:    " + nullSafe(profile.getPhone()));
        out.println("Telegram:   " + nullSafe(profile.getTelegram()));
        out.println("Портфолио:  " + nullSafe(profile.getPortfolioLinks()));
        if (!input.confirm("Изменить профиль?")) {
            return;
        }
        String name = input.readOptionalString("ФИО (Enter — без изменений) > ");
        String title = input.readOptionalString("Желаемая должность > ");
        String skills = input.readOptionalString("Навыки > ");
        String phone = input.readOptionalString("Телефон > ");
        String telegram = input.readOptionalString("Telegram > ");
        String links = input.readOptionalString("Портфолио / GitHub > ");
        candidateService.updateProfile(
                candidateProfileId,
                name.isBlank() ? null : name,
                title.isBlank() ? null : title,
                skills.isBlank() ? null : skills,
                phone.isBlank() ? null : phone,
                telegram.isBlank() ? null : telegram,
                links.isBlank() ? null : links
        );
        out.println("[OK] Профиль обновлён");
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
