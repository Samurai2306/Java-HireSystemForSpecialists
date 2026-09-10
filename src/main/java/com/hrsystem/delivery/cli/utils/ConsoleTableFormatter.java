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
import java.util.stream.Collectors;

public class ConsoleTableFormatter {

    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";
    public static final String BLUE = "\u001B[34m";
    public static final String CYAN = "\u001B[36m";
    public static final String GRAY = "\u001B[90m";

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

    public ConsoleTableFormatter setHeaders(String... headers) {
        this.headers.clear();
        this.headers.addAll(Arrays.asList(headers));
        return this;
    }

    public ConsoleTableFormatter addRow(String... cells) {
        this.rows.add(Arrays.asList(cells));
        return this;
    }

    public ConsoleTableFormatter addRow(List<String> cells) {
        this.rows.add(new ArrayList<>(cells));
        return this;
    }

    public static int visibleLength(String text) {
        if (text == null) return 0;
        return ANSI_PATTERN.matcher(text).replaceAll("").length();
    }

    public String render() {
        if (headers.isEmpty() && rows.isEmpty()) {
            return "(нет данных для отображения)";
        }

        int colCount = headers.size();
        for (List<String> row : rows) {
            colCount = Math.max(colCount, row.size());
        }

        int[] colWidths = new int[colCount];
        for (int i = 0; i < headers.size(); i++) {
            colWidths[i] = Math.max(colWidths[i], visibleLength(headers.get(i)));
        }

        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                colWidths[i] = Math.max(colWidths[i], visibleLength(row.get(i)));
            }
        }

        StringBuilder sb = new StringBuilder();
        String border = buildBorder(colWidths, '+', '-');
        sb.append(border).append("\n");

        if (!headers.isEmpty()) {
            sb.append("|");
            for (int i = 0; i < colCount; i++) {
                String h = i < headers.size() ? headers.get(i) : "";
                sb.append(" ").append(padRight(h, colWidths[i])).append(" |");
            }
            sb.append("\n");
            sb.append(buildBorder(colWidths, '+', '=')).append("\n");
        }

        if (rows.isEmpty()) {
            sb.append("|");
            int totalInnerWidth = 0;
            for (int w : colWidths) totalInnerWidth += (w + 3);
            totalInnerWidth -= 1;
            String emptyMsg = " Записи не найдены ";
            sb.append(padCenter(emptyMsg, totalInnerWidth)).append("|\n");
        } else {
            for (List<String> row : rows) {
                sb.append("|");
                for (int i = 0; i < colCount; i++) {
                    String cell = i < row.size() ? (row.get(i) != null ? row.get(i) : "") : "";
                    sb.append(" ").append(padRight(cell, colWidths[i])).append(" |");
                }
                sb.append("\n");
            }
        }

        sb.append(border);
        return sb.toString();
    }

    public String format(List<String> headerList, List<List<String>> rowList) {
        if (headerList == null || headerList.isEmpty()) {
            return "";
        }
        int columns = headerList.size();
        int[] widths = new int[columns];
        for (int i = 0; i < columns; i++) {
            widths[i] = visibleLength(headerList.get(i));
        }
        for (List<String> row : rowList) {
            for (int i = 0; i < columns; i++) {
                String cell = i < row.size() && row.get(i) != null ? row.get(i) : "";
                widths[i] = Math.max(widths[i], visibleLength(cell));
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(separator(widths)).append(System.lineSeparator());
        sb.append(rowLine(headerList, widths)).append(System.lineSeparator());
        sb.append(separator(widths)).append(System.lineSeparator());
        if (rowList.isEmpty()) {
            List<String> empty = new ArrayList<>();
            empty.add("нет данных");
            for (int i = 1; i < columns; i++) {
                empty.add("");
            }
            sb.append(rowLine(empty, widths)).append(System.lineSeparator());
        } else {
            for (List<String> row : rowList) {
                List<String> padded = new ArrayList<>();
                for (int i = 0; i < columns; i++) {
                    padded.add(i < row.size() && row.get(i) != null ? row.get(i) : "");
                }
                sb.append(rowLine(padded, widths)).append(System.lineSeparator());
            }
        }
        sb.append(separator(widths));
        return sb.toString();
    }

    public String formatVacancies(List<VacancyEntity> vacancies) {
        List<String> h = List.of("ID", "Должность", "Компания", "Зарплата", "Источник", "Дата");
        List<List<String>> r = vacancies.stream()
                .map(v -> List.of(
                        String.valueOf(v.getId()),
                        nullToEmpty(v.getTitle()),
                        nullToEmpty(v.getCompanyName()),
                        formatSalary(v),
                        sourceMarker(v.getSourceType()),
                        v.getPublishedAt() == null ? "" : DATE.format(v.getPublishedAt())
                ))
                .collect(Collectors.toList());
        return format(h, r);
    }

    public String formatEmployerVacancies(List<EmployerVacancyRowDto> rList) {
        List<String> h = List.of("ID", "Должность", "Дата", "Статус", "Откликов");
        List<List<String>> table = rList.stream()
                .map(r -> List.of(
                        String.valueOf(r.getId()),
                        nullToEmpty(r.getTitle()),
                        r.getPublishedAt() == null ? "" : r.getPublishedAt().length() >= 10 ? r.getPublishedAt().substring(0, 10) : r.getPublishedAt(),
                        vacancyStatusLabel(r.getStatus()),
                        String.valueOf(r.getApplicationCount())
                ))
                .collect(Collectors.toList());
        return format(h, table);
    }

    public String formatApplications(List<ApplicationEntity> applications) {
        List<String> h = List.of("ID", "Кандидат", "Контакты", "Дата", "Статус", "Письмо");
        List<List<String>> r = applications.stream()
                .map(a -> List.of(
                        String.valueOf(a.getId()),
                        a.getCandidate() == null ? "" : nullToEmpty(a.getCandidate().getFullName()),
                        candidateContacts(a),
                        a.getCreatedAt() == null ? "" : DATE.format(a.getCreatedAt()),
                        colorStatus(a.getStatus()),
                        truncate(nullToEmpty(a.getCoverLetter()), 40)
                ))
                .collect(Collectors.toList());
        return format(h, r);
    }

    public String formatCandidateApplications(List<ApplicationEntity> applications) {
        List<String> h = List.of("ID", "Должность", "Компания", "Дата", "Статус");
        List<List<String>> r = applications.stream()
                .map(a -> List.of(
                        String.valueOf(a.getId()),
                        a.getVacancy() == null ? "" : nullToEmpty(a.getVacancy().getTitle()),
                        a.getVacancy() == null ? "" : nullToEmpty(a.getVacancy().getCompanyName()),
                        a.getCreatedAt() == null ? "" : DATE.format(a.getCreatedAt()),
                        colorStatus(a.getStatus())
                ))
                .collect(Collectors.toList());
        return format(h, r);
    }

    public String formatLogs(List<ParsingLogEntity> logs) {
        List<String> h = List.of("ID", "Источник", "Старт", "Найдено", "Сохранено", "Дубли", "Статус");
        List<List<String>> r = logs.stream()
                .map(l -> List.of(
                        String.valueOf(l.getId()),
                        l.getSource() == null ? "—" : nullToEmpty(l.getSource().getName()),
                        l.getStartedAt() == null ? "" : DATE_TIME.format(l.getStartedAt()),
                        String.valueOf(l.getItemsFound()),
                        String.valueOf(l.getItemsSaved()),
                        String.valueOf(l.getDuplicatesSkipped()),
                        l.getStatus()
                ))
                .collect(Collectors.toList());
        return format(h, r);
    }

    public String formatSources(List<ParsingSourceEntity> sources) {
        List<String> h = List.of("ID", "Название", "Тип", "URL", "Активен", "Последний сбор");
        List<List<String>> r = sources.stream()
                .map(s -> List.of(
                        String.valueOf(s.getId()),
                        nullToEmpty(s.getName()),
                        String.valueOf(s.getSourceType()),
                        truncate(nullToEmpty(s.getBaseUrl()), 42),
                        s.isActive() ? "да" : "нет",
                        s.getLastScrapedAt() == null ? "—" : DATE_TIME.format(s.getLastScrapedAt())
                ))
                .collect(Collectors.toList());
        return format(h, r);
    }

    public String formatUsers(List<UserEntity> users) {
        List<String> h = List.of("ID", "Email", "Роль", "Активен");
        List<List<String>> r = users.stream()
                .map(u -> List.of(
                        String.valueOf(u.getId()),
                        nullToEmpty(u.getEmail()),
                        String.valueOf(u.getRole()),
                        u.isActive() ? "да" : "нет"
                ))
                .collect(Collectors.toList());
        return format(h, r);
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
            return formatNumber(vacancy.getSalaryMin()) + " - " + formatNumber(vacancy.getSalaryMax()) + " " + currency;
        }
        if (vacancy.getSalaryMin() != null) {
            return "от " + formatNumber(vacancy.getSalaryMin()) + " " + currency;
        }
        return "до " + formatNumber(vacancy.getSalaryMax()) + " " + currency;
    }

    public String colorStatus(ApplicationStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case OFFER -> GREEN + status.name() + RESET;
            case REVIEWING -> YELLOW + status.name() + RESET;
            case REJECTED -> RED + status.name() + RESET;
            case APPLIED -> BLUE + status.name() + RESET;
            case WITHDRAWN -> GRAY + status.name() + RESET;
        };
    }

    private String vacancyStatusLabel(VacancyStatus status) {
        if (status == VacancyStatus.ACTIVE) {
            return "Активна";
        }
        if (status == VacancyStatus.ARCHIVED) {
            return "В архиве";
        }
        return status == null ? "" : status.name();
    }

    private String candidateContacts(ApplicationEntity application) {
        if (application.getCandidate() == null) {
            return "";
        }
        String telegram = application.getCandidate().getTelegram();
        String phone = application.getCandidate().getPhone();
        if (telegram != null && !telegram.isBlank()) {
            return telegram;
        }
        return phone == null ? "" : phone;
    }

    private String separator(int[] widths) {
        return Arrays.stream(widths)
                .mapToObj(w -> "+" + "-".repeat(w + 2))
                .collect(Collectors.joining()) + "+";
    }

    private String rowLine(List<String> cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            String cell = cells.get(i);
            sb.append("| ").append(padVisible(cell, widths[i])).append(' ');
        }
        sb.append('|');
        return sb.toString();
    }

    private String padVisible(String text, int width) {
        int visible = visibleLength(text);
        if (visible >= width) {
            return text;
        }
        return text + " ".repeat(width - visible);
    }

    public String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String plain = text.replaceAll("\\s+", " ").trim();
        if (plain.length() <= max) {
            return plain;
        }
        return plain.substring(0, Math.max(0, max - 3)) + "...";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String formatNumber(int value) {
        return String.format(Locale.US, "%,d", value).replace(",", " ");
    }

    private String buildBorder(int[] widths, char corner, char line) {
        StringBuilder sb = new StringBuilder();
        sb.append(corner);
        for (int w : widths) {
            for (int j = 0; j < w + 2; j++) {
                sb.append(line);
            }
            sb.append(corner);
        }
        return sb.toString();
    }

    private String padRight(String text, int width) {
        if (text == null) text = "";
        int len = visibleLength(text);
        int padding = Math.max(0, width - len);
        StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < padding; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    private String padCenter(String text, int width) {
        if (text == null) text = "";
        int len = visibleLength(text);
        int totalPad = Math.max(0, width - len);
        int leftPad = totalPad / 2;
        int rightPad = totalPad - leftPad;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < leftPad; i++) sb.append(" ");
        sb.append(text);
        for (int i = 0; i < rightPad; i++) sb.append(" ");
        return sb.toString();
    }
}
