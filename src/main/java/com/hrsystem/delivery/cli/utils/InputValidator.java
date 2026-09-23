package com.hrsystem.delivery.cli.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Единая точка чтения пользовательского ввода. Один общий экземпляр-бин на всё приложение:
 * несколько Scanner поверх System.in конфликтуют и «съедают» строки друг друга.
 */
@Component
public class InputValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://).+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("[A-Za-z0-9.-]+\\.[A-Za-z]{2,}([/?#]\\S*)?");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d[\\d\\s\\-()]*");
    private static final Pattern TELEGRAM_PATTERN = Pattern.compile("@[A-Za-z0-9_]{3,32}");

    private final Scanner scanner;
    private final PrintStream out;

    @Autowired
    public InputValidator() {
        this(System.in, System.out);
    }

    public InputValidator(InputStream in, PrintStream out) {
        this(new Scanner(in), out);
    }

    public InputValidator(Scanner scanner, PrintStream out) {
        this.scanner = scanner;
        this.out = out;
    }

    public String readNonEmptyString(String prompt) {
        while (true) {
            String line = readRaw(prompt);
            if (line == null) {
                return "";
            }
            line = line.trim();
            if (!line.isEmpty()) {
                return line;
            }
            error("Поле не может быть пустым. Повторите ввод.");
        }
    }

    public String readRequiredString(String prompt) {
        return readNonEmptyString(prompt);
    }

    public String readOptionalString(String prompt, String defaultValue) {
        String line = readRaw(prompt);
        if (line == null) {
            return defaultValue;
        }
        line = line.trim();
        return line.isEmpty() ? defaultValue : line;
    }

    public String readOptionalString(String prompt) {
        return readOptionalString(prompt, "");
    }

    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            String line = readRaw(prompt);
            if (line == null) {
                return min;
            }
            try {
                int value = Integer.parseInt(line.trim());
                if (value >= min && value <= max) {
                    return value;
                }
                error("Число должно быть в диапазоне от " + min + " до " + max + ".");
            } catch (NumberFormatException e) {
                error("Некорректный ввод! Введите целое число.");
            }
        }
    }

    public Integer readOptionalInt(String prompt, Integer defaultValue) {
        while (true) {
            String line = readRaw(prompt);
            if (line == null) {
                return defaultValue;
            }
            line = line.trim();
            if (line.isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                error("Некорректный ввод! Введите целое число или нажмите Enter для пропуска.");
            }
        }
    }

    public Integer readOptionalInt(String prompt) {
        return readOptionalInt(prompt, null);
    }

    public long readLong(String prompt) {
        while (true) {
            String line = readRaw(prompt);
            if (line == null) {
                return 0L;
            }
            try {
                return Long.parseLong(line.trim());
            } catch (NumberFormatException ex) {
                error("Ожидается целое число (ID)");
            }
        }
    }

    public String readEmail(String prompt) {
        while (true) {
            String email = readNonEmptyString(prompt);
            if (isEmail(email)) {
                return email.toLowerCase(Locale.ROOT);
            }
            error("Неверный формат email (пример: user@domain.com). Повторите ввод.");
        }
    }

    public String readUrl(String prompt) {
        while (true) {
            String value = readNonEmptyString(prompt);
            if (isUrl(value)) {
                return value;
            }
            if (DOMAIN_PATTERN.matcher(value).matches()) {
                return "https://" + value;
            }
            error("Укажите ссылку вида https://site.com (можно и просто site.com).");
        }
    }

    /** Телефон: цифры, пробелы, скобки, дефис; от 10 до 15 цифр (например +7 (999) 123-45-67). */
    public String readPhone(String prompt) {
        while (true) {
            String value = readNonEmptyString(prompt).trim();
            if (isPhone(value)) {
                return value;
            }
            error("Неверный номер телефона. Пример: +7 (999) 123-45-67 (только цифры, 10-15 цифр).");
        }
    }

    /** Telegram-ник: @ + латиница/цифры/подчёркивание, например @username. */
    public String readTelegram(String prompt) {
        while (true) {
            String value = readNonEmptyString(prompt).trim();
            if (isTelegram(value)) {
                return value;
            }
            error("Telegram должен начинаться с @ и содержать только латиницу, цифры и _ (пример: @username).");
        }
    }

    public String readOptionalPhone(String prompt, String current) {
        while (true) {
            String line = readRaw(prompt);
            if (line == null || line.trim().isEmpty()) {
                return current;
            }
            String value = line.trim();
            if (isPhone(value)) {
                return value;
            }
            error("Неверный номер телефона. Пример: +7 (999) 123-45-67 (Enter — оставить текущий).");
        }
    }

    public String readOptionalTelegram(String prompt, String current) {
        while (true) {
            String line = readRaw(prompt);
            if (line == null || line.trim().isEmpty()) {
                return current;
            }
            String value = line.trim();
            if (isTelegram(value)) {
                return value;
            }
            error("Telegram должен начинаться с @ (пример: @username). Enter — оставить текущий.");
        }
    }

    public String readOptionalUrl(String prompt, String current) {
        while (true) {
            String line = readRaw(prompt);
            if (line == null || line.trim().isEmpty()) {
                return current;
            }
            String value = line.trim();
            if (isUrl(value)) {
                return value;
            }
            if (DOMAIN_PATTERN.matcher(value).matches()) {
                return "https://" + value;
            }
            error("Укажите ссылку вида https://site.com. Enter — оставить текущую.");
        }
    }

    public String readPassword(String prompt) {
        while (true) {
            String password = readRaw(prompt);
            if (password == null) {
                return "";
            }
            password = password.trim();
            if (password.length() >= 4) {
                return password;
            }
            error("Пароль должен содержать не менее 4 символов. Повторите ввод.");
        }
    }

    public String readCommand(String prompt) {
        String line = readRaw(prompt);
        return line == null ? "" : line.trim();
    }

    public boolean readConfirmation(String prompt, boolean defaultYes) {
        String line = readRaw(prompt + " " + (defaultYes ? "[Y/n]" : "[y/N]") + ": ");
        if (line == null || line.isBlank()) {
            return defaultYes;
        }
        line = line.trim().toLowerCase(Locale.ROOT);
        return line.startsWith("y") || line.startsWith("д") || line.equals("1");
    }

    public boolean confirm(String prompt) {
        return readConfirmation(prompt, false);
    }

    public boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value.trim()).matches();
    }

    public boolean isUrl(String value) {
        return value != null && URL_PATTERN.matcher(value.trim()).matches();
    }

    public boolean isPhone(String value) {
        if (value == null || !PHONE_PATTERN.matcher(value.trim()).matches()) {
            return false;
        }
        long digits = value.chars().filter(Character::isDigit).count();
        return digits >= 10 && digits <= 15;
    }

    public boolean isTelegram(String value) {
        return value != null && TELEGRAM_PATTERN.matcher(value.trim()).matches();
    }

    private String readRaw(String prompt) {
        out.print(prompt);
        out.flush();
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    private void error(String message) {
        out.println(AnsiColor.error(message));
    }
}
