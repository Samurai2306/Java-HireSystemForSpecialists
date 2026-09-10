package com.hrsystem.delivery.cli.views;

import com.hrsystem.delivery.cli.CliSessionContext;
import com.hrsystem.delivery.cli.utils.AnsiColor;
import com.hrsystem.delivery.cli.utils.ConsoleTableFormatter;
import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.dto.request.ApplyVacancyDto;
import com.hrsystem.dto.request.CandidateProfileUpdateDto;
import com.hrsystem.dto.request.VacancyFilterDto;
import com.hrsystem.dto.response.ApplicationDto;
import com.hrsystem.dto.response.VacancyDetailsDto;
import com.hrsystem.dto.response.VacancySummaryDto;
import com.hrsystem.exception.AccessDeniedException;
import com.hrsystem.exception.DuplicateApplicationException;
import com.hrsystem.exception.EntityNotFoundException;
import com.hrsystem.exception.InvalidStateTransitionException;
import com.hrsystem.exception.ValidationException;
import com.hrsystem.service.CandidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.util.List;

@Component
public class CandidateCliView {

    private final CandidateService candidateService;
    private final CliSessionContext sessionContext;
    private final InputValidator inputValidator;
    private final PrintStream out;

    @Autowired
    public CandidateCliView(CandidateService candidateService, CliSessionContext sessionContext) {
        this(candidateService, sessionContext, new InputValidator(), System.out);
    }

    public CandidateCliView(CandidateService candidateService, CliSessionContext sessionContext,
                            InputValidator inputValidator, PrintStream out) {
        this.candidateService = candidateService;
        this.sessionContext = sessionContext;
        this.inputValidator = inputValidator;
        this.out = out;
    }

    public void show(Long candidateProfileId, Long userId, InputValidator input, PrintStream out) {
        showCandidateDashboard();
    }

    public void showCandidateDashboard() {
        boolean inMenu = true;
        while (inMenu) {
            String userName = sessionContext.getCurrentCandidateProfile() != null
                    ? sessionContext.getCurrentCandidateProfile().getFullName()
                    : (sessionContext.getCurrentUser() != null ? sessionContext.getCurrentUser().getEmail() : "Гость");

            out.println("\n" + AnsiColor.colorize("================================================", AnsiColor.BLUE));
            out.println(AnsiColor.colorize("         ЛИЧНЫЙ КАБИНЕТ СОИСКАТЕЛЯ: " + userName, AnsiColor.BOLD + AnsiColor.BLUE));
            out.println(AnsiColor.colorize("================================================", AnsiColor.BLUE));
            out.println("  1. Каталог вакансий и фильтры");
            out.println("  2. Мои отклики и трекер заявок");
            out.println("  3. Мой профиль / Резюме");
            out.println("  0. Выход из аккаунта (Logout)");
            out.println(AnsiColor.colorize("------------------------------------------------", AnsiColor.GRAY));

            int choice = inputValidator.readIntInRange("Выберите раздел [0-3]: ", 0, 3);
            switch (choice) {
                case 1:
                    showVacancyCatalog();
                    break;
                case 2:
                    showMyApplications();
                    break;
                case 3:
                    showCandidateProfile();
                    break;
                case 0:
                    sessionContext.logout();
                    out.println(AnsiColor.info("Вы вышли из учетной записи."));
                    inMenu = false;
                    break;
            }
        }
    }

    public void showVacancyCatalog() {
        VacancyFilterDto filter = new VacancyFilterDto();
        boolean browsing = true;

        while (browsing) {
            Page<VacancySummaryDto> page = candidateService.searchVacancies(filter);

            out.println("\n" + AnsiColor.colorize("--- КАТАЛОГ ВАКАНСИЙ ---", AnsiColor.BOLD));
            printActiveFilters(filter);

            ConsoleTableFormatter table = new ConsoleTableFormatter("ID", "Должность", "Компания", "Зарплата", "Источник", "Дата");
            for (VacancySummaryDto v : page.getContent()) {
                table.addRow(
                        String.valueOf(v.getId()),
                        v.getTitle(),
                        v.getCompanyName(),
                        v.getSalaryFormatted(),
                        v.getSourceFormatted(),
                        v.getPublishedDateFormatted()
                );
            }
            out.println(table.render());

            int totalPages = Math.max(1, page.getTotalPages());
            out.println(String.format("Страница %s из %s (Всего вакансий: %s)",
                    AnsiColor.colorize(String.valueOf(filter.getPageNumber()), AnsiColor.BOLD),
                    totalPages,
                    page.getTotalElements()));

            out.println(AnsiColor.colorize("----------------------------------------------------------------------", AnsiColor.GRAY));
            out.println("Команды: [N] След. стр | [P] Пред. стр | [F] Фильтры | [C] Сброс фильтров");
            out.println("         [D] Детали вакансии | [O] Откликнуться | [B] Назад в меню");

            String cmd = inputValidator.readCommand("Введите команду: ").toUpperCase();
            switch (cmd) {
                case "N":
                    if (filter.getPageNumber() < totalPages) {
                        filter.setPageNumber(filter.getPageNumber() + 1);
                    } else {
                        out.println(AnsiColor.warning("Вы находитесь на последней странице."));
                    }
                    break;
                case "P":
                    if (filter.getPageNumber() > 1) {
                        filter.setPageNumber(filter.getPageNumber() - 1);
                    } else {
                        out.println(AnsiColor.warning("Вы находитесь на первой странице."));
                    }
                    break;
                case "F":
                    configureFilters(filter);
                    break;
                case "C":
                    filter = new VacancyFilterDto();
                    out.println(AnsiColor.info("Фильтры сброшены."));
                    break;
                case "D":
                    handleViewVacancyDetails();
                    break;
                case "O":
                    handleApplyForVacancy();
                    break;
                case "B":
                    browsing = false;
                    break;
                default:
                    out.println(AnsiColor.error("Неизвестная команда! Используйте N, P, F, C, D, O или B."));
                    break;
            }
        }
    }

    private void printActiveFilters(VacancyFilterDto filter) {
        StringBuilder sb = new StringBuilder("Активные фильтры: ");
        boolean hasAny = false;
        if (filter.hasKeyword()) {
            sb.append("Ключевое слово: '").append(filter.getKeyword()).append("' | ");
            hasAny = true;
        }
        if (filter.hasSalaryMin()) {
            sb.append("Зарплата от: ").append(filter.getSalaryMin()).append(" | ");
            hasAny = true;
        }
        if (filter.hasSourceFilter()) {
            sb.append("Источник: ").append(filter.getSource()).append(" | ");
            hasAny = true;
        }
        if (!hasAny) {
            sb.append("нет (все вакансии)");
        }
        out.println(AnsiColor.colorize(sb.toString(), AnsiColor.GRAY));
    }

    private void configureFilters(VacancyFilterDto filter) {
        out.println("\n" + AnsiColor.colorize("--- Настройка фильтрации вакансий ---", AnsiColor.BOLD));
        String kw = inputValidator.readOptionalString("Поиск по стеку / должности (оставьте пустым для пропуска): ", null);
        Integer minSal = inputValidator.readOptionalInt("Минимальная зарплата (или Enter для пропуска): ", null);

        out.println("Выберите источник:");
        out.println("  1. Все источники (ALL)");
        out.println("  2. Сайты (WEBSITE)");
        out.println("  3. Telegram (TELEGRAM)");
        out.println("  4. Работодатели вручную (MANUAL)");
        int srcChoice = inputValidator.readIntInRange("Выбор [1-4]: ", 1, 4);
        VacancySource source;
        switch (srcChoice) {
            case 2:
                source = VacancySource.WEBSITE;
                break;
            case 3:
                source = VacancySource.TELEGRAM;
                break;
            case 4:
                source = VacancySource.MANUAL;
                break;
            default:
                source = VacancySource.ALL;
                break;
        }

        filter.setKeyword(kw);
        filter.setSalaryMin(minSal);
        filter.setSource(source);
        filter.setPageNumber(1); // Reset to first page
        out.println(AnsiColor.success("Фильтры успешно применены!"));
    }

    private void handleViewVacancyDetails() {
        Integer id = inputValidator.readOptionalInt("Введите ID вакансии для просмотра деталей: ", null);
        if (id == null) return;

        try {
            VacancyDetailsDto d = candidateService.getVacancyDetails(Long.valueOf(id));
            out.println("\n" + AnsiColor.colorize("==================================================================", AnsiColor.CYAN));
            out.println(AnsiColor.colorize("  КАРТОЧКА ВАКАНСИИ #" + d.getId() + ": " + d.getTitle(), AnsiColor.BOLD + AnsiColor.CYAN));
            out.println(AnsiColor.colorize("==================================================================", AnsiColor.CYAN));
            out.println("Компания:       " + d.getCompanyName());
            out.println("Зарплата:       " + AnsiColor.colorize(d.getSalaryFormatted(), AnsiColor.GREEN));
            out.println("Локация:        " + (d.getLocation() != null ? d.getLocation() : "Не указана"));
            out.println("Формат работы:  " + (d.getEmploymentType() != null ? d.getEmploymentType() : "Не указан"));
            out.println("Источник:       " + d.getSourceFormatted() + (d.getSourceUrl() != null ? " (" + d.getSourceUrl() + ")" : ""));
            out.println("Дата публикации:" + d.getPublishedDateFormatted());
            out.println(AnsiColor.colorize("------------------------------------------------------------------", AnsiColor.GRAY));
            out.println(AnsiColor.colorize("Требуемый стек технологий:", AnsiColor.BOLD));
            out.println(d.getRequirementsStack() != null ? d.getRequirementsStack() : "Не указан");
            out.println(AnsiColor.colorize("------------------------------------------------------------------", AnsiColor.GRAY));
            out.println(AnsiColor.colorize("Описание вакансии:", AnsiColor.BOLD));
            out.println(d.getDescription() != null ? d.getDescription() : "Описание отсутствует");
            out.println(AnsiColor.colorize("==================================================================", AnsiColor.CYAN));

            if (sessionContext.isCandidate()) {
                boolean applyNow = inputValidator.readConfirmation("Желаете откликнуться на эту вакансию?", true);
                if (applyNow) {
                    processApplicationSubmission(d.getId());
                }
            }
        } catch (EntityNotFoundException | ValidationException e) {
            out.println(AnsiColor.error(e.getMessage()));
        }
    }

    private void handleApplyForVacancy() {
        if (!sessionContext.isCandidate()) {
            out.println(AnsiColor.error("Для подачи отклика необходимо авторизоваться как Соискатель."));
            return;
        }

        Integer id = inputValidator.readOptionalInt("Введите ID вакансии для отклика: ", null);
        if (id == null) return;
        processApplicationSubmission(Long.valueOf(id));
    }

    private void processApplicationSubmission(Long vacancyId) {
        out.println("\n" + AnsiColor.colorize("--- Подача отклика на вакансию #" + vacancyId + " ---", AnsiColor.BOLD));
        boolean useProfile = inputValidator.readConfirmation("Использовать стандартное резюме и текст из вашего профиля?", true);
        String coverLetter = null;

        if (!useProfile) {
            coverLetter = inputValidator.readNonEmptyString("Введите сопроводительное письмо: ");
        }

        ApplyVacancyDto dto = new ApplyVacancyDto(vacancyId, coverLetter, useProfile);
        try {
            ApplicationDto result = candidateService.applyForVacancy(sessionContext.getCurrentUserId(), dto);
            out.println(AnsiColor.success(String.format("Отклик #%d успешно отправлен со статусом %s!",
                    result.getId(), AnsiColor.colorizeStatus(result.getStatus()))));
        } catch (DuplicateApplicationException e) {
            out.println(AnsiColor.warning("Блокировка повторной подачи: " + e.getMessage()));
        } catch (EntityNotFoundException | ValidationException | AccessDeniedException e) {
            out.println(AnsiColor.error("Ошибка при подаче отклика: " + e.getMessage()));
        }
    }

    public void showMyApplications() {
        if (!sessionContext.isCandidate()) {
            out.println(AnsiColor.error("Доступно только авторизованному соискателю."));
            return;
        }

        boolean viewing = true;
        while (viewing) {
            List<ApplicationDto> list = candidateService.getMyApplications(sessionContext.getCurrentUserId());

            out.println("\n" + AnsiColor.colorize("--- РЕЕСТР: МОИ ОТКЛИКИ И ТРЕКЕР ЗАЯВОК ---", AnsiColor.BOLD));
            ConsoleTableFormatter table = new ConsoleTableFormatter(
                    "ID Заявки", "Должность", "Компания", "Дата подачи", "Текущий статус"
            );

            for (ApplicationDto a : list) {
                table.addRow(
                        String.valueOf(a.getId()),
                        a.getVacancyTitle(),
                        a.getCompanyName(),
                        a.getAppliedAtFormatted(),
                        AnsiColor.colorizeStatus(a.getStatus())
                );
            }
            out.println(table.render());

            out.println(AnsiColor.colorize("----------------------------------------------------------------------", AnsiColor.GRAY));
            out.println("Команды: [D] Детали отклика | [W] Отозвать отклик | [B] Назад в меню");

            String cmd = inputValidator.readCommand("Введите команду: ").toUpperCase();
            switch (cmd) {
                case "D":
                    handleViewApplicationDetails();
                    break;
                case "W":
                    handleWithdrawApplication();
                    break;
                case "B":
                    viewing = false;
                    break;
                default:
                    out.println(AnsiColor.error("Неизвестная команда! Используйте D, W или B."));
                    break;
            }
        }
    }

    private void handleViewApplicationDetails() {
        Integer appId = inputValidator.readOptionalInt("Введите ID отклика для просмотра: ", null);
        if (appId == null) return;

        try {
            ApplicationDto a = candidateService.getApplicationDetails(sessionContext.getCurrentUserId(), Long.valueOf(appId));
            out.println("\n" + AnsiColor.colorize("==================================================================", AnsiColor.CYAN));
            out.println(AnsiColor.colorize("  ДЕТАЛИ ОТКЛИКА #" + a.getId() + " НА ВАКАНСИЮ: " + a.getVacancyTitle(), AnsiColor.BOLD + AnsiColor.CYAN));
            out.println(AnsiColor.colorize("==================================================================", AnsiColor.CYAN));
            out.println("Компания:          " + a.getCompanyName());
            out.println("Статус отклика:    " + AnsiColor.colorizeStatus(a.getStatus()));
            out.println("Дата подачи:       " + a.getAppliedAtFormatted());
            out.println("Дата обновления:   " + a.getUpdatedAtFormatted());
            if (a.getStatusComment() != null && !a.getStatusComment().isEmpty()) {
                out.println("Комментарий HR:    " + AnsiColor.colorize(a.getStatusComment(), AnsiColor.YELLOW));
            }
            out.println(AnsiColor.colorize("------------------------------------------------------------------", AnsiColor.GRAY));
            out.println(AnsiColor.colorize("Отправленное сопроводительное письмо:", AnsiColor.BOLD));
            out.println(a.getCoverLetter() != null ? a.getCoverLetter() : "(пусто)");
            out.println(AnsiColor.colorize("==================================================================", AnsiColor.CYAN));
        } catch (EntityNotFoundException | AccessDeniedException | ValidationException e) {
            out.println(AnsiColor.error(e.getMessage()));
        }
    }

    private void handleWithdrawApplication() {
        Integer appId = inputValidator.readOptionalInt("Введите ID отклика, который хотите отозвать: ", null);
        if (appId == null) return;

        boolean confirm = inputValidator.readConfirmation("Вы уверены, что хотите отозвать заявку #" + appId + "?", false);
        if (!confirm) {
            out.println("Операция отзыва отменена.");
            return;
        }

        try {
            ApplicationDto updated = candidateService.withdrawApplication(sessionContext.getCurrentUserId(), Long.valueOf(appId));
            out.println(AnsiColor.success(String.format("Отклик #%d успешно отозван! Текущий статус: %s",
                    updated.getId(), AnsiColor.colorizeStatus(updated.getStatus()))));
        } catch (InvalidStateTransitionException e) {
            out.println(AnsiColor.warning(e.getMessage()));
        } catch (EntityNotFoundException | AccessDeniedException | ValidationException e) {
            out.println(AnsiColor.error(e.getMessage()));
        }
    }

    public void showCandidateProfile() {
        if (!sessionContext.isCandidate()) {
            out.println(AnsiColor.error("Доступно только авторизованному соискателю."));
            return;
        }

        CandidateProfileEntity p = candidateService.getProfile(sessionContext.getCurrentUserId());

        out.println("\n" + AnsiColor.colorize("==================================================================", AnsiColor.MAGENTA));
        out.println(AnsiColor.colorize("                      ПРОФИЛЬ СОИСКАТЕЛЯ                         ", AnsiColor.BOLD + AnsiColor.MAGENTA));
        out.println(AnsiColor.colorize("==================================================================", AnsiColor.MAGENTA));
        out.println("ФИО:               " + p.getFullName());
        out.println("Желаемая должность:" + (p.getTargetTitle() != null ? p.getTargetTitle() : "Не указана"));
        out.println("Стек и навыки:     " + (p.getSkills() != null ? p.getSkills() : "Не указаны"));
        out.println("Телефон:           " + (p.getPhone() != null ? p.getPhone() : "Не указан"));
        out.println("Telegram:          " + (p.getTelegram() != null ? p.getTelegram() : "Не указан"));
        out.println("Портфолио / GitHub:" + (p.getPortfolioLinks() != null ? p.getPortfolioLinks() : "Не указано"));
        out.println(AnsiColor.colorize("==================================================================", AnsiColor.MAGENTA));

        boolean edit = inputValidator.readConfirmation("Желаете обновить данные профиля?", false);
        if (edit) {
            handleEditProfile(p);
        }
    }

    private void handleEditProfile(CandidateProfileEntity current) {
        out.println("\n" + AnsiColor.colorize("--- Редактирование профиля (Enter - оставить текущее) ---", AnsiColor.BOLD));
        String name = inputValidator.readOptionalString("ФИО [" + current.getFullName() + "]: ", current.getFullName());
        String title = inputValidator.readOptionalString("Желаемая должность [" + (current.getTargetTitle() != null ? current.getTargetTitle() : "") + "]: ", current.getTargetTitle());
        String skills = inputValidator.readOptionalString("Стек и навыки [" + (current.getSkills() != null ? current.getSkills() : "") + "]: ", current.getSkills());
        String phone = inputValidator.readOptionalString("Телефон [" + (current.getPhone() != null ? current.getPhone() : "") + "]: ", current.getPhone());
        String telegram = inputValidator.readOptionalString("Telegram [" + (current.getTelegram() != null ? current.getTelegram() : "") + "]: ", current.getTelegram());
        String portfolio = inputValidator.readOptionalString("Портфолио [" + (current.getPortfolioLinks() != null ? current.getPortfolioLinks() : "") + "]: ", current.getPortfolioLinks());

        CandidateProfileUpdateDto dto = new CandidateProfileUpdateDto(name, title, skills, phone, telegram, portfolio);
        CandidateProfileEntity updated = candidateService.updateProfile(sessionContext.getCurrentUserId(), dto);
        sessionContext.updateCandidateProfile(updated);
        out.println(AnsiColor.success("Профиль успешно обновлен!"));
    }
}
