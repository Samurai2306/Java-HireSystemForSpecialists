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
        this.scanner = new Scanner(in);
        this.out = out;
    }

    public InputValidator(Scanner scanner, PrintStream out) {
        this.scanner = scanner;
        this.out = out;
    }

    public String readNonEmptyString(String prompt) {
        while (true) {
            out.print(prompt);
            out.flush();
            if (!scanner.hasNextLine()) {
                return "";
            }
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            out.println(AnsiColor.error("Поле не может быть пустым. Повторите ввод."));
        }
    }

    public String readRequiredString(String prompt) {
        return readNonEmptyString(prompt);
    }

    public String readOptionalString(String prompt, String defaultValue) {
        out.print(prompt);
        out.flush();
        if (!scanner.hasNextLine()) {
            return defaultValue;
        }
        String line = scanner.nextLine().trim();
        return line.isEmpty() ? defaultValue : line;
    }

    public String readOptionalString(String prompt) {
        out.print(prompt);
        out.flush();
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine().trim();
    }

    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            out.print(prompt);
            out.flush();
            if (!scanner.hasNextLine()) {
                return min;
            }
            String line = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                out.println(AnsiColor.error(String.format("Число должно быть в диапазоне от %d до %d.", min, max)));
            } catch (NumberFormatException e) {
                out.println(AnsiColor.error("Некорректный ввод! Введите целое число."));
            }
        }
    }

    public Integer readOptionalInt(String prompt, Integer defaultValue) {
        while (true) {
            out.print(prompt);
            out.flush();
            if (!scanner.hasNextLine()) {
                return defaultValue;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                out.println(AnsiColor.error("Некорректный ввод! Введите целое число или нажмите Enter для пропуска."));
            }
        }
    }

    public Integer readOptionalInt(String prompt) {
        return readOptionalInt(prompt, null);
    }

    public long readLong(String prompt) {
        while (true) {
            out.print(prompt);
            out.flush();
            if (!scanner.hasNextLine()) {
                return 0L;
            }
            String line = scanner.nextLine().trim();
            try {
                return Long.parseLong(line);
            } catch (NumberFormatException ex) {
                out.println(AnsiColor.error("Ожидается целое число (ID)"));
            }
        }
    }

    public String readEmail(String prompt) {
        while (true) {
            String email = readNonEmptyString(prompt);
            if (isEmail(email)) {
                return email.toLowerCase(Locale.ROOT);
            }
            out.println(AnsiColor.error("Неверный формат email (пример: user@domain.com). Повторите ввод."));
        }
    }

    public String readUrl(String prompt) {
        while (true) {
            String value = readNonEmptyString(prompt);
            if (isUrl(value)) {
                return value;
            }
            out.println(AnsiColor.error("URL должен начинаться с http:// или https://"));
        }
    }

    public String readPassword(String prompt) {
        while (true) {
            out.print(prompt);
            out.flush();
            if (!scanner.hasNextLine()) {
                return "";
            }
            String password = scanner.nextLine();
            if (password != null && password.trim().length() >= 4) {
                return password.trim();
            }
            out.println(AnsiColor.error("Пароль должен содержать не менее 4 символов. Повторите ввод."));
        }
    }

    public String readCommand(String prompt) {
        out.print(prompt);
        out.flush();
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine().trim();
    }

    public boolean readConfirmation(String prompt, boolean defaultYes) {
        String hint = defaultYes ? "[Y/n]" : "[y/N]";
        out.print(prompt + " " + hint + ": ");
        out.flush();
        if (!scanner.hasNextLine()) {
            return defaultYes;
        }
        String line = scanner.nextLine().trim().toLowerCase();
        if (line.isEmpty()) {
            return defaultYes;
        }
        return line.startsWith("y") || line.startsWith("д") || line.equals("1");
    }

    public boolean confirm(String prompt) {
        String answer = readOptionalString(prompt + " [y/N]: ");
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")
                || answer.equalsIgnoreCase("д") || answer.equalsIgnoreCase("да");
    }

    public boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value.trim()).matches();
    }

    public boolean isUrl(String value) {
        return value != null && URL_PATTERN.matcher(value.trim()).matches();
    }
}
