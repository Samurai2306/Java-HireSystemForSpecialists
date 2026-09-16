package com.hrsystem.delivery.cli.views;

import com.hrsystem.delivery.cli.utils.ConsoleTableFormatter;
import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.domain.entity.ParsingLogEntity;
import com.hrsystem.domain.entity.ParsingSourceEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.domain.enums.VacancyStatus;
import com.hrsystem.dto.response.DashboardStatsDto;
import com.hrsystem.dto.response.ParsingReportDto;
import com.hrsystem.repository.ParsingLogRepository;
import com.hrsystem.scraper.ScraperCoordinatorService;
import com.hrsystem.service.ModerationService;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.util.List;

@Component
public class AdminCliView {

    private final ModerationService moderationService;
    private final ScraperCoordinatorService scraperCoordinatorService;
    private final ParsingLogRepository parsingLogRepository;
    private final ConsoleTableFormatter tables = new ConsoleTableFormatter();

    public AdminCliView(ModerationService moderationService,
                        ScraperCoordinatorService scraperCoordinatorService,
                        ParsingLogRepository parsingLogRepository) {
        this.moderationService = moderationService;
        this.scraperCoordinatorService = scraperCoordinatorService;
        this.parsingLogRepository = parsingLogRepository;
    }

    public void show(InputValidator input, PrintStream out) {
        boolean inMenu = true;
        while (inMenu) {
            printDashboard(out);
            out.println("[1] Запустить принудительный парсинг HTML-сайтов");
            out.println("[2] Запустить парсинг Telegram-каналов");
            out.println("[3] Управление списком источников (Добавить / вкл-выкл)");
            out.println("[4] Журнал логов парсинга");
            out.println("[5] Модерация каталога вакансий");
            out.println("[6] Блокировка учётных записей");
            out.println("[0] Выход в главное меню (Logout)");
            try {
                switch (input.readIntInRange("Выберите команду > ", 0, 6)) {
                    case 1 -> runScraping(input, out, true);
                    case 2 -> runScraping(input, out, false);
                    case 3 -> manageSources(input, out);
                    case 4 -> showLogs(out);
                    case 5 -> moderateVacancies(input, out);
                    case 6 -> moderateUsers(input, out);
                    case 0 -> inMenu = false;
                    default -> {
                    }
                }
            } catch (RuntimeException ex) {
                out.println("[Ошибка] " + ex.getMessage());
            }
        }
    }

    private void printDashboard(PrintStream out) {
        DashboardStatsDto stats = moderationService.getDashboardStats();
        out.println();
        out.println("=== ПАНЕЛЬ УПРАВЛЕНИЯ ПАРСЕРАМИ И СБОРОМ ДАННЫХ ===");
        out.println("Статистика базы данных:");
        out.println("- Всего активных вакансий: " + stats.activeVacancies());
        out.println("- Спарсено через Web Scraper: " + stats.websiteVacancies());
        out.println("- Спарсено через Telegram Mirror: " + stats.telegramVacancies());
        out.println("- Добавлено работодателями вручную: " + stats.manualVacancies());
        out.println("- Последний сбор: " + stats.lastParsingStartedAt() + " (" + stats.lastParsingStatus() + ")");
        out.println("------------------------------------------------------------");
    }

    private void runScraping(InputValidator input, PrintStream out, boolean website) {
        out.println();
        out.println(website ? ">>> Запуск HTML Web Scraper..." : ">>> Запуск Telegram Web Mirror Extractor...");
        ParsingReportDto report = website
                ? scraperCoordinatorService.runWebsiteScraping(out::println)
                : scraperCoordinatorService.runTelegramScraping(out::println);
        out.println();
        out.println("=== ОТЧЁТ ПАРСИНГА ===");
        out.println("Найдено:            " + report.getItemsFound());
        out.println("Добавлено новых:    " + report.getItemsSaved());
        out.println("Отсеяно дубликатов: " + report.getDuplicatesSkipped());
        out.println("Ошибок источников:  " + report.getFailedSources());
        if (report.getSummary() != null) {
            out.println(report.getSummary());
        }
        input.readOptionalString("Enter для продолжения > ");
    }

    private void manageSources(InputValidator input, PrintStream out) {
        boolean inSources = true;
        while (inSources) {
            List<ParsingSourceEntity> sources = moderationService.listSources();
            out.println();
            out.println("=== ИСТОЧНИКИ СБОРА ===");
            out.println(tables.formatSources(sources));
            out.println("[1] Добавить URL  [2] Включить/выключить  [0] Назад");
            int action = input.readIntInRange("Действие > ", 0, 2);
            if (action == 0) {
                inSources = false;
            } else if (action == 1) {
                ParsingSourceEntity created = moderationService.addSource(
                        input.readRequiredString("Название > "),
                        input.readIntInRange("Тип [1] WEBSITE [2] TELEGRAM > ", 1, 2) == 2
                                ? VacancySource.TELEGRAM : VacancySource.WEBSITE,
                        input.readUrl("URL > ")
                );
                out.println("[OK] Источник #" + created.getId() + " добавлен");
            } else {
                ParsingSourceEntity updated = moderationService.toggleSource(input.readLong("ID источника > "));
                out.println("[OK] Источник #" + updated.getId() + " активен=" + updated.isActive());
            }
        }
    }

    private void showLogs(PrintStream out) {
        List<ParsingLogEntity> logs = parsingLogRepository.findTop20ByOrderByStartedAtDesc();
        out.println();
        out.println("=== ЖУРНАЛ ПАРСИНГА ===");
        out.println(tables.formatLogs(logs));
        for (ParsingLogEntity log : logs) {
            if (log.getErrorMessage() != null && !log.getErrorMessage().isBlank()) {
                out.println("Лог #" + log.getId() + " ошибка: " + log.getErrorMessage());
            }
        }
    }

    private void moderateVacancies(InputValidator input, PrintStream out) {
        List<VacancyEntity> vacancies = moderationService.listRecentVacancies();
        out.println();
        out.println("=== МОДЕРАЦИЯ ВАКАНСИЙ ===");
        out.println(tables.formatVacancies(vacancies));
        out.println("[1] В архив (скрыть)  [2] Отклонить  [3] Вернуть ACTIVE  [0] Назад");
        int action = input.readIntInRange("Действие > ", 0, 3);
        if (action == 0) {
            return;
        }
        VacancyStatus status = switch (action) {
            case 2 -> VacancyStatus.REJECTED;
            case 3 -> VacancyStatus.ACTIVE;
            default -> VacancyStatus.ARCHIVED;
        };
        VacancyEntity updated = moderationService.changeVacancyStatus(input.readLong("ID вакансии > "), status);
        out.println("[OK] Вакансия #" + updated.getId() + " → " + updated.getStatus());
    }

    private void moderateUsers(InputValidator input, PrintStream out) {
        List<UserEntity> users = moderationService.listUsers();
        out.println();
        out.println("=== ПОЛЬЗОВАТЕЛИ ===");
        out.println(tables.formatUsers(users));
        out.println("[1] Заблокировать  [2] Разблокировать  [0] Назад");
        int action = input.readIntInRange("Действие > ", 0, 2);
        if (action == 0) {
            return;
        }
        UserEntity updated = moderationService.setUserActive(input.readLong("ID пользователя > "), action == 2);
        out.println("[OK] Пользователь " + updated.getEmail() + " активен=" + updated.isActive());
    }
}
