package com.hrsystem.scraper;

import com.hrsystem.domain.enums.Currency;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SalaryParser {

    public record ParsedSalary(Integer min, Integer max, Currency currency) {
    }

    private static final Pattern RANGE = Pattern.compile(
            "(?:от\\s*)?[$€₽₸]?\\s*(?<min>\\d[\\d\\s.,]*)\\s*(?:k|к|тыс\\.?)?\\s*(?:[-–—]|до)\\s*[$€₽₸]?\\s*(?<max>\\d[\\d\\s.,]*)\\s*(?:k|к|тыс\\.?)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern FROM = Pattern.compile(
            "от\\s*(?<min>\\d[\\d\\s.,]*)\\s*(?:k|к|тыс\\.?)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern UP_TO = Pattern.compile(
            "до\\s*(?<max>\\d[\\d\\s.,]*)\\s*(?:k|к|тыс\\.?)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern SINGLE = Pattern.compile("(?<val>\\d[\\d\\s.,]*)\\s*(?:k|к|тыс\\.?)?");

    public ParsedSalary parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedSalary(null, null, Currency.RUB);
        }
        String text = raw.toLowerCase(Locale.ROOT).replace('\u00A0', ' ');
        Currency currency = detectCurrency(text);
        boolean thousandsShortcut = text.matches("(?s).*\\d\\s*(k|к|тыс).*") || text.contains("тыс");

        Matcher range = RANGE.matcher(text);
        if (range.find()) {
            Integer min = toNumber(range.group("min"), thousandsShortcut || looksLikeK(range.group("min"), text));
            Integer max = toNumber(range.group("max"), thousandsShortcut || looksLikeK(range.group("max"), text));
            return new ParsedSalary(min, max, currency);
        }
        Matcher from = FROM.matcher(text);
        if (from.find()) {
            return new ParsedSalary(toNumber(from.group("min"), thousandsShortcut), null, currency);
        }
        Matcher upTo = UP_TO.matcher(text);
        if (upTo.find()) {
            return new ParsedSalary(null, toNumber(upTo.group("max"), thousandsShortcut), currency);
        }
        Matcher single = SINGLE.matcher(text);
        if (single.find()) {
            Integer value = toNumber(single.group("val"), thousandsShortcut);
            return new ParsedSalary(value, value, currency);
        }
        return new ParsedSalary(null, null, currency);
    }

    private boolean looksLikeK(String numberGroup, String full) {
        return full.contains("k") || full.contains("к");
    }

    private Currency detectCurrency(String text) {
        if (text.contains("$") || text.contains("usd") || text.contains("долл")) {
            return Currency.USD;
        }
        if (text.contains("€") || text.contains("eur") || text.contains("евро")) {
            return Currency.EUR;
        }
        if (text.contains("₸") || text.contains("kzt") || text.contains("тенге")) {
            return Currency.KZT;
        }
        return Currency.RUB;
    }

    private Integer toNumber(String raw, boolean multiplyThousands) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("[\\s.,]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(digits);
            if (multiplyThousands && value < 1000) {
                value *= 1000;
            }
            return value;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
