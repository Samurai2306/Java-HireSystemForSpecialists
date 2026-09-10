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
import java.util.stream.Collectors;

public class ConsoleTableFormatter {

    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";
    public static final String BLUE = "\u001B[34m";
    public static final String CYAN = "\u001B[36m";
    public static final String GRAY = "\u001B[90m";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    public String format(List<String> headers, List<List<String>> rows) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }
        int columns = headers.size();
        int[] widths = new int[columns];
        for (int i = 0; i < columns; i++) {
            widths[i] = visibleLength(headers.get(i));
        }
        for (List<String> row : rows) {
            for (int i = 0; i < columns; i++) {
                String cell = i < row.size() && row.get(i) != null ? row.get(i) : "";
                widths[i] = Math.max(widths[i], visibleLength(cell));
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(separator(widths)).append(System.lineSeparator());
        sb.append(rowLine(headers, widths)).append(System.lineSeparator());
        sb.append(separator(widths)).append(System.lineSeparator());
        if (rows.isEmpty()) {
            List<String> empty = new ArrayList<>();
            empty.add("нет данных");
            for (int i = 1; i < columns; i++) {
                empty.add("");
            }
            sb.append(rowLine(empty, widths)).append(System.lineSeparator());
        } else {
            for (List<String> row : rows) {
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
        List<String> headers = List.of("ID", "Должность", "Компания", "Зарплата", "Источник", "Дата");
        List<List<String>> rows = vacancies.stream()
                .map(v -> List.of(
                        String.valueOf(v.getId()),
                        nullToEmpty(v.getTitle()),
                        nullToEmpty(v.getCompanyName()),
                        formatSalary(v),
                        sourceMarker(v.getSourceType()),
                        v.getPublishedAt() == null ? "" : DATE.format(v.getPublishedAt())
                ))
                .collect(Collectors.toList());
        return format(headers, rows);
    }

    public String formatEmployerVacancies(List<EmployerVacancyRowDto> rows) {
        List<String> headers = List.of("ID", "Должность", "Дата", "Статус", "Откликов");
        List<List<String>> table = rows.stream()
                .map(r -> List.of(
                        String.valueOf(r.getId()),
                        nullToEmpty(r.getTitle()),
                        r.getPublishedAt() == null ? "" : r.getPublishedAt().length() >= 10 ? r.getPublishedAt().substring(0, 10) : r.getPublishedAt(),
                        vacancyStatusLabel(r.getStatus()),
                        String.valueOf(r.getApplicationCount())
                ))
                .collect(Collectors.toList());
        return format(headers, table);
    }

    public String formatApplications(List<ApplicationEntity> applications) {
        List<String> headers = List.of("ID", "Кандидат", "Контакты", "Дата", "Статус", "Письмо");
        List<List<String>> rows = applications.stream()
                .map(a -> List.of(
                        String.valueOf(a.getId()),
                        a.getCandidate() == null ? "" : nullToEmpty(a.getCandidate().getFullName()),
                        candidateContacts(a),
                        a.getCreatedAt() == null ? "" : DATE.format(a.getCreatedAt()),
                        colorStatus(a.getStatus()),
                        truncate(nullToEmpty(a.getCoverLetter()), 40)
                ))
                .collect(Collectors.toList());
        return format(headers, rows);
    }

    public String formatCandidateApplications(List<ApplicationEntity> applications) {
        List<String> headers = List.of("ID", "Должность", "Компания", "Дата", "Статус");
        List<List<String>> rows = applications.stream()
                .map(a -> List.of(
                        String.valueOf(a.getId()),
                        a.getVacancy() == null ? "" : nullToEmpty(a.getVacancy().getTitle()),
                        a.getVacancy() == null ? "" : nullToEmpty(a.getVacancy().getCompanyName()),
                        a.getCreatedAt() == null ? "" : DATE.format(a.getCreatedAt()),
                        colorStatus(a.getStatus())
                ))
                .collect(Collectors.toList());
        return format(headers, rows);
    }

    public String formatLogs(List<ParsingLogEntity> logs) {
        List<String> headers = List.of("ID", "Источник", "Старт", "Найдено", "Сохранено", "Дубли", "Статус");
        List<List<String>> rows = logs.stream()
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
        return format(headers, rows);
    }

    public String formatSources(List<ParsingSourceEntity> sources) {
        List<String> headers = List.of("ID", "Название", "Тип", "URL", "Активен", "Последний сбор");
        List<List<String>> rows = sources.stream()
                .map(s -> List.of(
                        String.valueOf(s.getId()),
                        nullToEmpty(s.getName()),
                        String.valueOf(s.getSourceType()),
                        truncate(nullToEmpty(s.getBaseUrl()), 42),
                        s.isActive() ? "да" : "нет",
                        s.getLastScrapedAt() == null ? "—" : DATE_TIME.format(s.getLastScrapedAt())
                ))
                .collect(Collectors.toList());
        return format(headers, rows);
    }

    public String formatUsers(List<UserEntity> users) {
        List<String> headers = List.of("ID", "Email", "Роль", "Активен");
        List<List<String>> rows = users.stream()
                .map(u -> List.of(
                        String.valueOf(u.getId()),
                        nullToEmpty(u.getEmail()),
                        String.valueOf(u.getRole()),
                        u.isActive() ? "да" : "нет"
                ))
                .collect(Collectors.toList());
        return format(headers, rows);
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

    int visibleLength(String text) {
        if (text == null) {
            return 0;
        }
        return text.replaceAll("\u001B\\[[;\\d]*m", "").length();
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
        return String.format(java.util.Locale.US, "%,d", value).replace(",", " ");
    }
}
