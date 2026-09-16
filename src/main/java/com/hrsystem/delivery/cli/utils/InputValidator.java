package com.hrsystem.delivery.cli.utils;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

public class InputValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://).+", Pattern.CASE_INSENSITIVE);

    private final Scanner scanner;
    private final PrintStream out;

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
            error("URL должен начинаться с http:// или https://");
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

    private String readRaw(String prompt) {
        out.print(prompt);
        out.flush();
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    private void error(String message) {
        out.println(AnsiColor.error(message));
    }
}
