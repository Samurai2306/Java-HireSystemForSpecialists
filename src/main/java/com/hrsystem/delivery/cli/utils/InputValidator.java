package com.hrsystem.delivery.cli.utils;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

public class InputValidator {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern URL = Pattern.compile("^(https?://).+", Pattern.CASE_INSENSITIVE);

    private final Scanner scanner;
    private final PrintStream out;

    public InputValidator(Scanner scanner, PrintStream out) {
        this.scanner = scanner;
        this.out = out;
    }

    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            out.print(prompt);
            out.flush();
            String line = readLine();
            try {
                int value = Integer.parseInt(line.trim());
                if (value < min || value > max) {
                    out.println("[!] Введите число от " + min + " до " + max);
                    continue;
                }
                return value;
            } catch (NumberFormatException ex) {
                out.println("[!] Ожидается целое число, а не: " + line);
            }
        }
    }

    public Integer readOptionalInt(String prompt) {
        while (true) {
            out.print(prompt);
            out.flush();
            String line = readLine();
            if (line.isBlank()) {
                return null;
            }
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException ex) {
                out.println("[!] Ожидается целое число или пустая строка");
            }
        }
    }

    public long readLong(String prompt) {
        while (true) {
            out.print(prompt);
            out.flush();
            String line = readLine();
            try {
                return Long.parseLong(line.trim());
            } catch (NumberFormatException ex) {
                out.println("[!] Ожидается целое число (ID)");
            }
        }
    }

    public String readRequiredString(String prompt) {
        while (true) {
            out.print(prompt);
            out.flush();
            String line = readLine();
            if (!line.isBlank()) {
                return line.trim();
            }
            out.println("[!] Поле не может быть пустым");
        }
    }

    public String readOptionalString(String prompt) {
        out.print(prompt);
        out.flush();
        String line = readLine();
        return line.isBlank() ? "" : line.trim();
    }

    public String readEmail(String prompt) {
        while (true) {
            String value = readRequiredString(prompt);
            if (isEmail(value)) {
                return value.toLowerCase(Locale.ROOT);
            }
            out.println("[!] Некорректный email");
        }
    }

    public String readUrl(String prompt) {
        while (true) {
            String value = readRequiredString(prompt);
            if (isUrl(value)) {
                return value;
            }
            out.println("[!] URL должен начинаться с http:// или https://");
        }
    }

    public String readPassword(String prompt) {
        return readRequiredString(prompt);
    }

    public boolean confirm(String prompt) {
        String answer = readOptionalString(prompt + " [y/N]: ");
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")
                || answer.equalsIgnoreCase("д") || answer.equalsIgnoreCase("да");
    }

    public boolean isEmail(String value) {
        return value != null && EMAIL.matcher(value.trim()).matches();
    }

    public boolean isUrl(String value) {
        return value != null && URL.matcher(value.trim()).matches();
    }

    private String readLine() {
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine();
    }
}
