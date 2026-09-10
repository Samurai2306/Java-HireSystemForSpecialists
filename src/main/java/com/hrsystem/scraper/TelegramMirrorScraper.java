package com.hrsystem.scraper;

import com.hrsystem.domain.entity.ParsingSourceEntity;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.dto.parser.RawParsedVacancyDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class TelegramMirrorScraper {

    private static final Set<String> KEYWORDS = Set.of(
            "вакансия", "ищем", "требуется", "junior", "middle", "senior",
            "developer", "разработчик", "java", "python", "frontend", "backend",
            "qa", "devops", "зарплата", "оклад", "remote", "удалён"
    );

    private final HttpFetcher httpFetcher;
    private final TextCleaner textCleaner;

    public TelegramMirrorScraper(HttpFetcher httpFetcher, TextCleaner textCleaner) {
        this.httpFetcher = httpFetcher;
        this.textCleaner = textCleaner;
    }

    public List<RawParsedVacancyDto> scrape(ParsingSourceEntity source) throws IOException {
        Document document = httpFetcher.fetch(normalizeMirrorUrl(source.getBaseUrl()));
        Elements wraps = document.select(".tgme_widget_message_wrap, .tgme_widget_message");
        List<RawParsedVacancyDto> result = new ArrayList<>();
        for (Element wrap : wraps) {
            Element textEl = wrap.selectFirst(".tgme_widget_message_text");
            if (textEl == null) {
                continue;
            }
            String body = textCleaner.cleanHtml(textEl.html());
            if (body.isBlank() || !looksLikeVacancy(body)) {
                continue;
            }
            RawParsedVacancyDto dto = new RawParsedVacancyDto();
            dto.setTitle(extractTitle(body));
            dto.setCompanyName(extractCompany(body));
            dto.setDescription(body);
            dto.setSalaryRaw(body);
            dto.setRequirementsStack(body);
            dto.setSourceType(VacancySource.TELEGRAM);
            dto.setSourceUrl(extractPostUrl(wrap));
            dto.setPublishedAt(extractDate(wrap));
            dto.setLocation("Remote / Telegram");
            result.add(dto);
        }
        return result;
    }

    private boolean looksLikeVacancy(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return KEYWORDS.stream().anyMatch(lower::contains);
    }

    private String extractTitle(String body) {
        String firstLine = body.lines().filter(line -> !line.isBlank()).findFirst().orElse(body);
        return firstLine.length() > 120 ? firstLine.substring(0, 117) + "..." : firstLine;
    }

    private String extractCompany(String body) {
        for (String line : body.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("компания")
                    || trimmed.toLowerCase(Locale.ROOT).startsWith("company")) {
                return trimmed.replaceFirst("(?i)компания[:\\-\\s]*", "")
                        .replaceFirst("(?i)company[:\\-\\s]*", "")
                        .trim();
            }
        }
        return "Telegram";
    }

    private String extractPostUrl(Element wrap) {
        Element link = wrap.selectFirst("a.tgme_widget_message_date[href], a[href*=/]");
        return link == null ? null : link.absUrl("href");
    }

    private Instant extractDate(Element wrap) {
        Element time = wrap.selectFirst("time[datetime]");
        if (time != null) {
            try {
                return Instant.parse(time.attr("datetime"));
            } catch (Exception ignored) {
                // fallback below
            }
        }
        return Instant.now();
    }

    private String normalizeMirrorUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.startsWith("https://t.me/s/")) {
            return trimmed;
        }
        if (trimmed.startsWith("https://t.me/")) {
            return trimmed.replace("https://t.me/", "https://t.me/s/");
        }
        return trimmed;
    }
}
