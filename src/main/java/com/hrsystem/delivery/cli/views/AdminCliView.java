package com.hrsystem.delivery.cli.views;

import com.hrsystem.delivery.cli.CliSessionContext;
import com.hrsystem.delivery.cli.utils.ConsoleTableFormatter;
import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.domain.entity.ParsingLogEntity;
import com.hrsystem.domain.entity.ParsingSourceEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.domain.enums.VacancyStatus;
import com.hrsystem.dto.response.DashboardStatsDto;
import com.hrsystem.dto.response.ParsingReportDto;
import com.hrsystem.repository.ParsingLogRepository;
import com.hrsystem.scraper.ScraperCoordinatorService;
import com.hrsystem.service.ModerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.util.List;

/** Кабинет администратора: парсинг, модерация вакансий, блокировка пользователей. */
@Component
public class AdminCliView extends AbstractCliView implements RoleMenu {

    private static final String MENU =
            "[1] Запустить принудительный парсинг HTML-сайтов\n"
                    + "[2] Запустить парсинг Telegram-каналов\n"
                    + "[3] Управление списком источников (Добавить / вкл-выкл)\n"
                    + "[4] Журнал логов парсинга\n"
                    + "[5] Модерация каталога вакансий\n"
                    + "[6] Блокировка учётных записей\n"
                    + "[0] Выход в главное меню (Logout)";

    private final ModerationService moderationService;
    private final ScraperCoordinatorService scraperCoordinatorService;
    private final ParsingLogRepository parsingLogRepository;
    private final ConsoleTableFormatter tables = new ConsoleTableFormatter();

    @Autowired
    public AdminCliView(ModerationService moderationService,
                        ScraperCoordinatorService scraperCoordinatorService,
                        ParsingLogRepository parsingLogRepository,
                        CliSessionContext sessionContext,
                        InputValidator input) {
        this(moderationService, scraperCoordinatorService, parsingLogRepository, sessionContext, input, System.out);
    }

    public AdminCliView(ModerationService moderationService,
                        ScraperCoordinatorService scraperCoordinatorService,
                        ParsingLogRepository parsingLogRepository,
                        CliSessionContext sessionContext,
                        InputValidator input,
                        PrintStream out) {
        super(sessionContext, input, out);
        this.moderationService = moderationService;
        this.scraperCoordinatorService = scraperCoordinatorService;
        this.parsingLogRepository = parsingLogRepository;
    }

    @Override
    public boolean supports(UserRole role) {
        return role == UserRole.ADMIN;
    }

    @Override
    public void open() {
        while (true) {
            safely(this::printDashboard);
            out.println(MENU);
            switch (in.readIntInRange("Выберите команду > ", 0, 6)) {
                case 1 -> safely(() -> runScraping(true));
                case 2 -> safely(() -> runScraping(false));
                case 3 -> safely(this::manageSources);
                case 4 -> safely(this::showLogs);
                case 5 -> safely(this::moderateVacancies);
                case 6 -> safely(this::moderateUsers);
                case 0 -> {
                    return;
                }
                default -> {
                }
            }
        }
    }

    private void printDashboard() {
        DashboardStatsDto stats = moderationService.getDashboardStats();
        out.println("\n=== ПАНЕЛЬ УПРАВЛЕНИЯ ПАРСЕРАМИ И СБОРОМ ДАННЫХ ===\n"
                + "Статистика базы данных:\n"
                + "- Всего активных вакансий: " + stats.activeVacancies() + "\n"
                + "- Спарсено через Web Scraper: " + stats.websiteVacancies() + "\n"
                + "- Спарсено через Telegram Mirror: " + stats.telegramVacancies() + "\n"
                + "- Добавлено работодателями вручную: " + stats.manualVacancies() + "\n"
                + "- Последний сбор: " + stats.lastParsingStartedAt() + " (" + stats.lastParsingStatus() + ")\n"
                + LINE);
    }

    private void runScraping(boolean website) {
        out.println("\n" + (website
                ? ">>> Запуск HTML Web Scraper..."
                : ">>> Запуск Telegram Web Mirror Extractor..."));
        ParsingReportDto report = website
                ? scraperCoordinatorService.runWebsiteScraping(out::println)
                : scraperCoordinatorService.runTelegramScraping(out::println);
        out.println("\n=== ОТЧЁТ ПАРСИНГА ===\n"
                + "Найдено:            " + report.getItemsFound() + "\n"
                + "Добавлено новых:    " + report.getItemsSaved() + "\n"
                + "Отсеяно дубликатов: " + report.getDuplicatesSkipped() + "\n"
                + "Ошибок источников:  " + report.getFailedSources()
                + (report.getSummary() != null ? "\n" + report.getSummary() : ""));
        pause();
    }

    private void manageSources() {
        while (true) {
            List<ParsingSourceEntity> sources = moderationService.listSources();
            out.println("\n=== ИСТОЧНИКИ СБОРА ===\n"
                    + tables.formatSources(sources)
                    + "\n[1] Добавить URL  [2] Включить/выключить  [0] Назад");
            int action = in.readIntInRange("Действие > ", 0, 2);
            if (action == 0) {
                return;
            }
            if (action == 1) {
                ParsingSourceEntity created = moderationService.addSource(
                        in.readRequiredString("Название > "),
                        in.readIntInRange("Тип [1] WEBSITE [2] TELEGRAM > ", 1, 2) == 2
                                ? VacancySource.TELEGRAM : VacancySource.WEBSITE,
                        in.readUrl("URL > "));
                out.println("[OK] Источник #" + created.getId() + " добавлен");
            } else {
                ParsingSourceEntity updated = moderationService.toggleSource(in.readLong("ID источника > "));
                out.println("[OK] Источник #" + updated.getId() + " активен=" + updated.isActive());
            }
        }
    }

    private void showLogs() {
        List<ParsingLogEntity> logs = parsingLogRepository.findTop20ByOrderByStartedAtDesc();
        StringBuilder text = new StringBuilder("\n=== ЖУРНАЛ ПАРСИНГА ===\n").append(tables.formatLogs(logs));
        for (ParsingLogEntity log : logs) {
            if (log.getErrorMessage() != null && !log.getErrorMessage().isBlank()) {
                text.append("\nЛог #").append(log.getId()).append(" ошибка: ").append(log.getErrorMessage());
            }
        }
        out.println(text);
    }

    private void moderateVacancies() {
        List<VacancyEntity> vacancies = moderationService.listRecentVacancies();
        out.println("\n=== МОДЕРАЦИЯ ВАКАНСИЙ ===\n"
                + tables.formatVacancies(vacancies)
                + "\n[1] В архив (скрыть)  [2] Отклонить  [3] Вернуть ACTIVE  [0] Назад");
        int action = in.readIntInRange("Действие > ", 0, 3);
        if (action == 0) {
            return;
        }
        VacancyStatus status = switch (action) {
            case 2 -> VacancyStatus.REJECTED;
            case 3 -> VacancyStatus.ACTIVE;
            default -> VacancyStatus.ARCHIVED;
        };
        VacancyEntity updated = moderationService.changeVacancyStatus(in.readLong("ID вакансии > "), status);
        out.println("[OK] Вакансия #" + updated.getId() + " → " + updated.getStatus());
    }

    private void moderateUsers() {
        List<UserEntity> users = moderationService.listUsers();
        out.println("\n=== ПОЛЬЗОВАТЕЛИ ===\n"
                + tables.formatUsers(users)
                + "\n[1] Заблокировать  [2] Разблокировать  [0] Назад");
        int action = in.readIntInRange("Действие > ", 0, 2);
        if (action == 0) {
            return;
        }
        UserEntity updated = moderationService.setUserActive(in.readLong("ID пользователя > "), action == 2);
        out.println("[OK] Пользователь " + updated.getEmail() + " активен=" + updated.isActive());
    }
}
