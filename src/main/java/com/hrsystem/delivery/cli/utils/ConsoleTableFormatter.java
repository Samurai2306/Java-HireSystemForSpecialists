package com.hrsystem.delivery.cli.utils;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.entity.ParsingLogEntity;
import com.hrsystem.domain.entity.ParsingSourceEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.domain.enums.VacancyStatus;
import com.hrsystem.dto.response.EmployerVacancyRowDto;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class ConsoleTableFormatter {

    private static final Pattern ANSI_PATTERN = Pattern.compile("\\u001B\\[[;\\d]*m");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final List<String> headers = new ArrayList<>();
    private final List<List<String>> rows = new ArrayList<>();

    public ConsoleTableFormatter() {
    }

    public ConsoleTableFormatter(String... headers) {
        this.headers.addAll(Arrays.asList(headers));
    }

    public ConsoleTableFormatter addRow(String... cells) {
        rows.add(Arrays.asList(cells));
        return this;
    }

    public static int visibleLength(String text) {
        return text == null ? 0 : ANSI_PATTERN.matcher(text).replaceAll("").length();
    }

    public String render() {
        if (headers.isEmpty() && rows.isEmpty()) {
            return "(нет данных для отображения)";
        }
        int cols = headers.size();
        for (List<String> row : rows) {
            cols = Math.max(cols, row.size());
        }
        int[] widths = new int[cols];
        for (int i = 0; i < headers.size(); i++) {
            widths[i] = Math.max(widths[i], visibleLength(headers.get(i)));
        }
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                widths[i] = Math.max(widths[i], visibleLength(row.get(i)));
            }
        }

        StringBuilder sb = new StringBuilder();
        String border = border(widths, '-');
        sb.append(border).append('\n');
        if (!headers.isEmpty()) {
            sb.append(row(headers, widths)).append('\n');
            sb.append(border(widths, '=')).append('\n');
        }
        if (rows.isEmpty()) {
            int inner = 0;
            for (int w : widths) {
                inner += w + 3;
            }
            sb.append('|').append(pad(" Записи не найдены ", inner - 1, true)).append("|\n");
        } else {
            for (List<String> row : rows) {
                sb.append(row(row, widths)).append('\n');
            }
        }
        sb.append(border);
        return sb.toString();
    }

    public String formatVacancies(List<VacancyEntity> vacancies) {
        ConsoleTableFormatter table = new ConsoleTableFormatter("ID", "Должность", "Компания", "Зарплата", "Источник", "Дата");
        for (VacancyEntity v : vacancies) {
            table.addRow(
                    String.valueOf(v.getId()),
                    text(v.getTitle()),
                    text(v.getCompanyName()),
                    formatSalary(v),
                    sourceMarker(v.getSourceType()),
                    v.getPublishedAt() == null ? "" : DATE.format(v.getPublishedAt())
            );
        }
        return table.render();
    }

    public String formatEmployerVacancies(List<EmployerVacancyRowDto> items) {
        ConsoleTableFormatter table = new ConsoleTableFormatter("ID", "Должность", "Дата", "Статус", "Откликов");
        for (EmployerVacancyRowDto row : items) {
            String published = "";
            if (row.getPublishedAt() != null) {
                published = row.getPublishedAt();
            }
            if (published.length() >= 10) {
                published = published.substring(0, 10);
            }
            String statusLabel = "";
            if (row.getStatus() == VacancyStatus.ARCHIVED) {
                statusLabel = "В архиве";
            } else if (row.getStatus() == VacancyStatus.ACTIVE) {
                statusLabel = "Активна";
            } else if (row.getStatus() != null) {
                statusLabel = row.getStatus().name();
            }
            table.addRow(
                    String.valueOf(row.getId()),
                    text(row.getTitle()),
                    published,
                    statusLabel,
                    String.valueOf(row.getApplicationCount())
            );
        }
        return table.render();
    }

    public String formatApplications(List<ApplicationEntity> applications) {
        ConsoleTableFormatter table = new ConsoleTableFormatter("ID", "Кандидат", "Контакты", "Дата", "Статус", "Письмо");
        for (ApplicationEntity a : applications) {
            table.addRow(
                    String.valueOf(a.getId()),
                    a.getCandidate() == null ? "" : text(a.getCandidate().getFullName()),
                    contacts(a),
                    a.getCreatedAt() == null ? "" : DATE.format(a.getCreatedAt()),
                    colorStatus(a.getStatus()),
                    cut(text(a.getCoverLetter()), 40)
            );
        }
        return table.render();
    }

    public String formatLogs(List<ParsingLogEntity> logs) {
        ConsoleTableFormatter table = new ConsoleTableFormatter("ID", "Источник", "Старт", "Найдено", "Сохранено", "Дубли", "Статус");
        for (ParsingLogEntity log : logs) {
            table.addRow(
                    String.valueOf(log.getId()),
                    log.getSource() == null ? "—" : text(log.getSource().getName()),
                    log.getStartedAt() == null ? "" : DATE_TIME.format(log.getStartedAt()),
                    String.valueOf(log.getItemsFound()),
                    String.valueOf(log.getItemsSaved()),
                    String.valueOf(log.getDuplicatesSkipped()),
                    log.getStatus()
            );
        }
        return table.render();
    }

    public String formatSources(List<ParsingSourceEntity> sources) {
        ConsoleTableFormatter table = new ConsoleTableFormatter("ID", "Название", "Тип", "URL", "Активен", "Последний сбор");
        for (ParsingSourceEntity source : sources) {
            table.addRow(
                    String.valueOf(source.getId()),
                    text(source.getName()),
                    String.valueOf(source.getSourceType()),
                    cut(text(source.getBaseUrl()), 42),
                    source.isActive() ? "да" : "нет",
                    source.getLastScrapedAt() == null ? "—" : DATE_TIME.format(source.getLastScrapedAt())
            );
        }
        return table.render();
    }

    public String formatUsers(List<UserEntity> users) {
        ConsoleTableFormatter table = new ConsoleTableFormatter("ID", "Email", "Роль", "Активен");
        for (UserEntity user : users) {
            table.addRow(
                    String.valueOf(user.getId()),
                    text(user.getEmail()),
                    String.valueOf(user.getRole()),
                    user.isActive() ? "да" : "нет"
            );
        }
        return table.render();
    }

    public String sourceMarker(VacancySource source) {
        if (source == VacancySource.WEBSITE) {
            return "[Сайт]";
        }
        if (source == VacancySource.TELEGRAM) {
            return "[Telegram]";
        }
        return "[Прямой работодатель]";
    }

    public String formatSalary(VacancyEntity vacancy) {
        String currency = vacancy.getCurrency() == null ? "RUB" : vacancy.getCurrency().name();
        if (vacancy.getSalaryMin() == null && vacancy.getSalaryMax() == null) {
            return "не указана";
        }
        if (vacancy.getSalaryMin() != null && vacancy.getSalaryMax() != null) {
            return num(vacancy.getSalaryMin()) + " - " + num(vacancy.getSalaryMax()) + " " + currency;
        }
        if (vacancy.getSalaryMin() != null) {
            return "от " + num(vacancy.getSalaryMin()) + " " + currency;
        }
        return "до " + num(vacancy.getSalaryMax()) + " " + currency;
    }

    public String colorStatus(ApplicationStatus status) {
        if (status == null) {
            return "";
        }
        if (status == ApplicationStatus.OFFER) {
            return AnsiColor.GREEN + status.name() + AnsiColor.RESET;
        }
        if (status == ApplicationStatus.REVIEWING) {
            return AnsiColor.YELLOW + status.name() + AnsiColor.RESET;
        }
        if (status == ApplicationStatus.REJECTED) {
            return AnsiColor.RED + status.name() + AnsiColor.RESET;
        }
        if (status == ApplicationStatus.APPLIED) {
            return AnsiColor.BLUE + status.name() + AnsiColor.RESET;
        }
        return AnsiColor.GRAY + status.name() + AnsiColor.RESET;
    }

    private static String row(List<String> cells, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String cell = i < cells.size() && cells.get(i) != null ? cells.get(i) : "";
            sb.append(' ').append(pad(cell, widths[i], false)).append(" |");
        }
        return sb.toString();
    }

    private static String border(int[] widths, char line) {
        StringBuilder sb = new StringBuilder("+");
        for (int width : widths) {
            sb.append(String.valueOf(line).repeat(width + 2)).append('+');
        }
        return sb.toString();
    }

    private static String pad(String text, int width, boolean center) {
        int visible = visibleLength(text);
        int extra = Math.max(0, width - visible);
        if (!center) {
            return text + " ".repeat(extra);
        }
        int left = extra / 2;
        return " ".repeat(left) + text + " ".repeat(extra - left);
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static String cut(String text, int max) {
        String plain = text.replaceAll("\\s+", " ").trim();
        return plain.length() <= max ? plain : plain.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static String num(int value) {
        return String.format(Locale.US, "%,d", value).replace(",", " ");
    }

    private static String contacts(ApplicationEntity application) {
        if (application.getCandidate() == null) {
            return "";
        }
        String telegram = application.getCandidate().getTelegram();
        return telegram != null && !telegram.isBlank() ? telegram : text(application.getCandidate().getPhone());
    }
}
