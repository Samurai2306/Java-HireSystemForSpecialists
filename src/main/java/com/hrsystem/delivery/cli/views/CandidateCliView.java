package com.hrsystem.delivery.cli.views;

import com.hrsystem.delivery.cli.CliSessionContext;
import com.hrsystem.delivery.cli.utils.AnsiColor;
import com.hrsystem.delivery.cli.utils.ConsoleTableFormatter;
import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.dto.request.ApplyVacancyDto;
import com.hrsystem.dto.request.CandidateProfileUpdateDto;
import com.hrsystem.dto.request.VacancyFilterDto;
import com.hrsystem.dto.response.ApplicationDto;
import com.hrsystem.dto.response.VacancyDetailsDto;
import com.hrsystem.dto.response.VacancySummaryDto;
import com.hrsystem.service.CandidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.util.List;

/** Личный кабинет соискателя и гостевой каталог вакансий. */
@Component
public class CandidateCliView extends AbstractCliView implements RoleMenu {

    private static final List<String> CATALOG_COMMANDS = List.of(
            "Команды: [N] След. стр | [P] Пред. стр | [F] Фильтры | [C] Сброс фильтров",
            "         [D] Детали вакансии | [O] Откликнуться | [B] Назад в меню");

    private final CandidateService candidateService;

    @Autowired
    public CandidateCliView(CandidateService candidateService, CliSessionContext sessionContext, InputValidator input) {
        this(candidateService, sessionContext, input, System.out);
    }

    public CandidateCliView(CandidateService candidateService, CliSessionContext sessionContext,
                            InputValidator input, PrintStream out) {
        super(sessionContext, input, out);
        this.candidateService = candidateService;
    }

    @Override
    public boolean supports(UserRole role) {
        return role == UserRole.CANDIDATE || role == UserRole.GUEST;
    }

    @Override
    public void open() {
        if (sessionContext.isCandidate()) {
            showCandidateDashboard();
        } else {
            showVacancyCatalog();
        }
    }

    public void showCandidateDashboard() {
        while (true) {
            String userName = sessionContext.getCurrentCandidateProfile() != null
                    ? sessionContext.getCurrentCandidateProfile().getFullName()
                    : sessionContext.getCurrentUser().getEmail();
            header("ЛИЧНЫЙ КАБИНЕТ СОИСКАТЕЛЯ: " + userName, AnsiColor.BLUE);
            out.println("  1. Каталог вакансий и фильтры");
            out.println("  2. Мои отклики и трекер заявок");
            out.println("  3. Мой профиль / Резюме");
            out.println("  0. Выход из аккаунта (Logout)");
            out.println(AnsiColor.colorize(LINE, AnsiColor.GRAY));

            switch (in.readIntInRange("Выберите раздел [0-3]: ", 0, 3)) {
                case 1 -> safely(this::showVacancyCatalog);
                case 2 -> safely(this::showMyApplications);
                case 3 -> safely(this::showCandidateProfile);
                case 0 -> {
                    sessionContext.logout();
                    info("Вы вышли из учетной записи.");
                    return;
                }
                default -> {
                }
            }
        }
    }

    public void showVacancyCatalog() {
        VacancyFilterDto filter = new VacancyFilterDto();
        while (true) {
            Page<VacancySummaryDto> page = candidateService.searchVacancies(filter);

            header("КАТАЛОГ ВАКАНСИЙ");
            printActiveFilters(filter);
            ConsoleTableFormatter table = new ConsoleTableFormatter("ID", "Должность", "Компания", "Зарплата", "Источник", "Дата");
            for (VacancySummaryDto v : page.getContent()) {
                table.addRow(String.valueOf(v.getId()), v.getTitle(), v.getCompanyName(),
                        v.getSalaryFormatted(), v.getSourceFormatted(), v.getPublishedDateFormatted());
            }
            out.println(table.render());

            int totalPages = Math.max(1, page.getTotalPages());
            out.println("Страница " + filter.getPageNumber() + " из " + totalPages
                    + " (Всего вакансий: " + page.getTotalElements() + ")");
            out.println(AnsiColor.colorize(LINE, AnsiColor.GRAY));
            CATALOG_COMMANDS.forEach(out::println);

            switch (in.readCommand("Введите команду: ").toUpperCase()) {
                case "N" -> nextPage(filter, totalPages);
                case "P" -> previousPage(filter);
                case "F" -> configureFilters(filter);
                case "C" -> {
                    filter = new VacancyFilterDto();
                    info("Фильтры сброшены.");
                }
                case "D" -> safely(this::handleViewVacancyDetails);
                case "O" -> safely(this::handleApplyForVacancy);
                case "B" -> {
                    return;
                }
                default -> err("Неизвестная команда! Используйте N, P, F, C, D, O или B.");
            }
        }
    }

    private void nextPage(VacancyFilterDto filter, int totalPages) {
        if (filter.getPageNumber() < totalPages) {
            filter.setPageNumber(filter.getPageNumber() + 1);
        } else {
            warn("Вы находитесь на последней странице.");
        }
    }

    private void previousPage(VacancyFilterDto filter) {
        if (filter.getPageNumber() > 1) {
            filter.setPageNumber(filter.getPageNumber() - 1);
        } else {
            warn("Вы находитесь на первой странице.");
        }
    }

    private void printActiveFilters(VacancyFilterDto filter) {
        StringBuilder text = new StringBuilder("Активные фильтры: ");
        boolean hasAny = false;
        if (filter.hasKeyword()) {
            text.append("Ключевое слово: '").append(filter.getKeyword()).append("' | ");
            hasAny = true;
        }
        if (filter.hasSalaryMin()) {
            text.append("Зарплата от: ").append(filter.getSalaryMin()).append(" | ");
            hasAny = true;
        }
        if (filter.hasSourceFilter()) {
            text.append("Источник: ").append(filter.getSource()).append(" | ");
            hasAny = true;
        }
        out.println(AnsiColor.colorize(hasAny ? text.toString() : text + "нет (все вакансии)", AnsiColor.GRAY));
    }

    private void configureFilters(VacancyFilterDto filter) {
        out.println("\n" + AnsiColor.colorize("--- Настройка фильтрации вакансий ---", AnsiColor.BOLD));
        String keyword = in.readOptionalString("Поиск по стеку / должности (Enter — пропустить): ", null);
        Integer minSalary = in.readOptionalInt("Минимальная зарплата (Enter — пропустить): ", null);

        out.println("Выберите источник: 1. Все  2. Сайты  3. Telegram  4. Работодатели");
        VacancySource source = switch (in.readIntInRange("Выбор [1-4]: ", 1, 4)) {
            case 2 -> VacancySource.WEBSITE;
            case 3 -> VacancySource.TELEGRAM;
            case 4 -> VacancySource.MANUAL;
            default -> VacancySource.ALL;
        };

        filter.setKeyword(keyword);
        filter.setSalaryMin(minSalary);
        filter.setSource(source);
        filter.setPageNumber(1);
        ok("Фильтры успешно применены!");
    }

    private void handleViewVacancyDetails() {
        Integer id = in.readOptionalInt("Введите ID вакансии для просмотра деталей: ", null);
        if (id == null) {
            warn("ID вакансии не указан.");
            return;
        }

        VacancyDetailsDto d = candidateService.getVacancyDetails((long) id);
        header("КАРТОЧКА ВАКАНСИИ #" + d.getId() + ": " + d.getTitle());
        out.println("Компания:        " + d.getCompanyName());
        out.println("Зарплата:        " + AnsiColor.colorize(d.getSalaryFormatted(), AnsiColor.GREEN));
        out.println("Локация:         " + orDash(d.getLocation()));
        out.println("Формат работы:   " + (d.getEmploymentType() != null ? d.getEmploymentType() : "Не указан"));
        out.println("Источник:        " + d.getSourceFormatted()
                + (d.getSourceUrl() != null ? " (" + d.getSourceUrl() + ")" : ""));
        out.println("Дата публикации: " + d.getPublishedDateFormatted());
        out.println(AnsiColor.colorize(LINE, AnsiColor.GRAY));
        out.println(AnsiColor.colorize("Требуемый стек технологий:", AnsiColor.BOLD));
        out.println(orDash(d.getRequirementsStack()));
        out.println(AnsiColor.colorize(LINE, AnsiColor.GRAY));
        out.println(AnsiColor.colorize("Описание вакансии:", AnsiColor.BOLD));
        out.println(orDash(d.getDescription()));
        out.println(AnsiColor.colorize(DOUBLE_LINE, AnsiColor.CYAN));

        if (sessionContext.isCandidate() && in.readConfirmation("Желаете откликнуться на эту вакансию?", true)) {
            processApplicationSubmission(d.getId());
        }
    }

    private void handleApplyForVacancy() {
        if (!sessionContext.isCandidate()) {
            err("Для подачи отклика необходимо авторизоваться как Соискатель.");
            return;
        }
        Integer id = in.readOptionalInt("Введите ID вакансии для отклика: ", null);
        if (id == null) {
            warn("ID вакансии не указан.");
            return;
        }
        processApplicationSubmission((long) id);
    }

    private void processApplicationSubmission(Long vacancyId) {
        out.println("\n" + AnsiColor.colorize("--- Подача отклика на вакансию #" + vacancyId + " ---", AnsiColor.BOLD));
        boolean useProfile = in.readConfirmation(
                "Отправить отклик со стандартным резюме из профиля? (нет — ввести письмо вручную)", true);
        String coverLetter = useProfile ? null : in.readRequiredString("Введите сопроводительное письмо: ");

        ApplicationDto result = candidateService.applyForVacancy(
                sessionContext.getCurrentUserId(), new ApplyVacancyDto(vacancyId, coverLetter, useProfile));
        ok(String.format("Отклик #%d успешно отправлен со статусом %s!",
                result.getId(), AnsiColor.colorizeStatus(result.getStatus())));
    }

    public void showMyApplications() {
        if (!sessionContext.isCandidate()) {
            err("Доступно только авторизованному соискателю.");
            return;
        }

        while (true) {
            List<ApplicationDto> list = candidateService.getMyApplications(sessionContext.getCurrentUserId());
            header("РЕЕСТР: МОИ ОТКЛИКИ И ТРЕКЕР ЗАЯВОК");
            ConsoleTableFormatter table = new ConsoleTableFormatter(
                    "ID Заявки", "Должность", "Компания", "Дата подачи", "Текущий статус");
            for (ApplicationDto a : list) {
                table.addRow(String.valueOf(a.getId()), a.getVacancyTitle(), a.getCompanyName(),
                        a.getAppliedAtFormatted(), AnsiColor.colorizeStatus(a.getStatus()));
            }
            out.println(table.render());
            out.println(AnsiColor.colorize(LINE, AnsiColor.GRAY));
            out.println("Команды: [D] Детали отклика | [W] Отозвать отклик | [B] Назад в меню");

            switch (in.readCommand("Введите команду: ").toUpperCase()) {
                case "D" -> safely(this::handleViewApplicationDetails);
                case "W" -> safely(this::handleWithdrawApplication);
                case "B" -> {
                    return;
                }
                default -> err("Неизвестная команда! Используйте D, W или B.");
            }
        }
    }

    private void handleViewApplicationDetails() {
        Integer appId = in.readOptionalInt("Введите ID отклика для просмотра: ", null);
        if (appId == null) {
            warn("ID отклика не указан.");
            return;
        }

        ApplicationDto a = candidateService.getApplicationDetails(sessionContext.getCurrentUserId(), (long) appId);
        header("ДЕТАЛИ ОТКЛИКА #" + a.getId() + " НА ВАКАНСИЮ: " + a.getVacancyTitle());
        out.println("Компания:        " + a.getCompanyName());
        out.println("Статус отклика:  " + AnsiColor.colorizeStatus(a.getStatus()));
        out.println("Дата подачи:     " + a.getAppliedAtFormatted());
        out.println("Дата обновления: " + a.getUpdatedAtFormatted());
        if (a.getStatusComment() != null && !a.getStatusComment().isEmpty()) {
            out.println("Комментарий HR:  " + AnsiColor.colorize(a.getStatusComment(), AnsiColor.YELLOW));
        }
        out.println(AnsiColor.colorize(LINE, AnsiColor.GRAY));
        out.println(AnsiColor.colorize("Отправленное сопроводительное письмо:", AnsiColor.BOLD));
        out.println(a.getCoverLetter() != null ? a.getCoverLetter() : "(пусто)");
        out.println(AnsiColor.colorize(DOUBLE_LINE, AnsiColor.CYAN));
    }

    private void handleWithdrawApplication() {
        Integer appId = in.readOptionalInt("Введите ID отклика, который хотите отозвать: ", null);
        if (appId == null) {
            warn("ID отклика не указан.");
            return;
        }
        if (!in.readConfirmation("Вы уверены, что хотите отозвать заявку #" + appId + "?", false)) {
            out.println("Операция отзыва отменена.");
            return;
        }
        ApplicationDto updated = candidateService.withdrawApplication(sessionContext.getCurrentUserId(), (long) appId);
        ok(String.format("Отклик #%d успешно отозван! Текущий статус: %s",
                updated.getId(), AnsiColor.colorizeStatus(updated.getStatus())));
    }

    public void showCandidateProfile() {
        if (!sessionContext.isCandidate()) {
            err("Доступно только авторизованному соискателю.");
            return;
        }

        CandidateProfileEntity p = candidateService.getProfile(sessionContext.getCurrentUserId());
        header("ПРОФИЛЬ СОИСКАТЕЛЯ", AnsiColor.MAGENTA);
        out.println("ФИО:                " + p.getFullName());
        out.println("Желаемая должность: " + orDash(p.getTargetTitle()));
        out.println("Стек и навыки:      " + orDash(p.getSkills()));
        out.println("Телефон:            " + orDash(p.getPhone()));
        out.println("Telegram:           " + orDash(p.getTelegram()));
        out.println("Портфолио / GitHub: " + orDash(p.getPortfolioLinks()));
        out.println(AnsiColor.colorize(DOUBLE_LINE, AnsiColor.MAGENTA));

        if (in.readConfirmation("Желаете обновить данные профиля?", false)) {
            handleEditProfile(p);
        }
    }

    private void handleEditProfile(CandidateProfileEntity current) {
        out.println("\n" + AnsiColor.colorize("--- Редактирование профиля (Enter — оставить текущее) ---", AnsiColor.BOLD));
        CandidateProfileUpdateDto dto = new CandidateProfileUpdateDto(
                in.readOptionalString("ФИО [" + current.getFullName() + "]: ", current.getFullName()),
                in.readOptionalString("Желаемая должность [" + orEmpty(current.getTargetTitle()) + "]: ", current.getTargetTitle()),
                in.readOptionalString("Стек и навыки [" + orEmpty(current.getSkills()) + "]: ", current.getSkills()),
                in.readOptionalPhone("Телефон [" + orEmpty(current.getPhone()) + "]: ", current.getPhone()),
                in.readOptionalTelegram("Telegram [" + orEmpty(current.getTelegram()) + "]: ", current.getTelegram()),
                in.readOptionalUrl("Портфолио [" + orEmpty(current.getPortfolioLinks()) + "]: ", current.getPortfolioLinks())
        );
        sessionContext.updateCandidateProfile(
                candidateService.updateProfile(sessionContext.getCurrentUserId(), dto));
        ok("Профиль успешно обновлен!");
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "Не указано" : value;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
