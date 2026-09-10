package com.hrsystem.scraper;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class TextCleaner {

    private static final Pattern UTM = Pattern.compile("[?&](utm_[^=]+|yclid|fbclid|gclid)=[^&\\s]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern HASHTAG_SPAM = Pattern.compile("(?:#[\\p{L}\\d_]+){3,}");
    private static final Pattern MULTI_SPACE = Pattern.compile("[ \\t\\x0B\\f\\r]+");

    public String cleanHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String text = Jsoup.parse(html).text();
        return normalizeWhitespace(stripNoise(text));
    }

    public String cleanText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return normalizeWhitespace(stripNoise(raw));
    }

    private String stripNoise(String text) {
        String withoutUtm = UTM.matcher(text).replaceAll("");
        return HASHTAG_SPAM.matcher(withoutUtm).replaceAll("");
    }

    private String normalizeWhitespace(String text) {
        return MULTI_SPACE.matcher(text.replace('\u00A0', ' '))
                .replaceAll(" ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
