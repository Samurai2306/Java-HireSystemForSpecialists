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

/**
 * Парсер публичного веб-превью Telegram-канала (t.me/s/&lt;канал&gt;).
 *
 * Важно: превью отдаётся только для существующего публичного канала. Для мёртвого канала
 * Telegram делает редирект на t.me/&lt;канал&gt; и возвращает заглушку с кодом 200 — поэтому
 * страница обязательно проверяется ({@link #verifyPublicPreview}), иначе парсер молча
 * возвращал бы 0 карточек и в отчёте было бы «ошибок: 0».
 */
@Component
public class TelegramMirrorScraper {

    private static final Set<String> KEYWORDS = Set.of(
            "вакансия", "ищем", "требуется", "junior", "middle", "senior",
            "developer", "разработчик", "java", "python", "frontend", "backend",
            "qa", "devops", "зарплата", "оклад", "remote", "удалён"
    );


    private static final String[] FIELD_MARKERS = {"💰", "🏢", "📍", "🏠", "📋", "🔗", "📩", "🕒", "✅"};

    private final HtmlFetcher httpFetcher;
    private final TextCleaner textCleaner;

    public TelegramMirrorScraper(HtmlFetcher httpFetcher, TextCleaner textCleaner) {
        this.httpFetcher = httpFetcher;
        this.textCleaner = textCleaner;
    }

    public List<RawParsedVacancyDto> scrape(ParsingSourceEntity source) throws IOException {
        Document document = httpFetcher.fetch(normalizeMirrorUrl(source.getBaseUrl()));
        verifyPublicPreview(document, source);

   
        Elements wraps = document.select(".tgme_widget_message_wrap");
        if (wraps.isEmpty()) {
            wraps = document.select(".tgme_widget_message");
        }
        if (wraps.isEmpty()) {
            throw new IOException("Страница " + document.location() + " загружена, но сообщений не найдено. "
                    + "Проверьте, что канал " + source.getBaseUrl() + " существует и является публичным.");
        }

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
            result.add(toDto(body, wrap));
        }
        return result;
    }

  
    private void verifyPublicPreview(Document document, ParsingSourceEntity source) throws IOException {
        String location = document.location();
        if (!location.isBlank() && !location.contains("/s/")) {
            throw new IOException("У канала " + source.getBaseUrl() + " нет публичного веб-превью: Telegram вернул "
                    + location + " вместо страницы t.me/s/... Проверьте, что канал существует и открыт для веб-просмотра.");
        }
    }

    private RawParsedVacancyDto toDto(String body, Element wrap) {
        RawParsedVacancyDto dto = new RawParsedVacancyDto();
        dto.setTitle(extractTitle(body));
        dto.setCompanyName(extractCompany(body));
        dto.setSalaryRaw(extractField(body, "💰"));
        dto.setLocation(extractLocation(body));
        dto.setRequirementsStack(extractStack(body));
        dto.setDescription(body);
        dto.setSourceType(VacancySource.TELEGRAM);
        dto.setSourceUrl(extractPostUrl(wrap));
        dto.setPublishedAt(extractDate(wrap));
        return dto;
    }

    private boolean looksLikeVacancy(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return KEYWORDS.stream().anyMatch(lower::contains);
    }

   
    private String extractTitle(String body) {
        String head = body.substring(0, firstMarkerIndex(body));
        String title = textCleaner.cleanTitle(head);
        if (title.isBlank()) {
            title = textCleaner.cleanTitle(firstLine(body));
        }
        title = title.replaceFirst("^\\d{1,2}[.)]\\s*", "");
        return title.isBlank() ? "Вакансия из Telegram" : title;
    }

    private String extractCompany(String body) {
        String company = extractField(body, "🏢");
        if (company != null) {
            return company;
        }
        for (String line : body.split("\\R")) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith("компания") || lower.startsWith("company")) {
                return trimmed.replaceFirst("(?i)компания[:\\-\\s]*", "")
                        .replaceFirst("(?i)company[:\\-\\s]*", "")
                        .trim();
            }
        }
        return null;
    }

    private String extractLocation(String body) {
        String location = extractField(body, "📍");
        if (location == null) {
            location = extractField(body, "🏠");
        }
        return location == null ? "Remote / Telegram" : location;
    }

    private String extractStack(String body) {
        String stack = extractField(body, "📋");
        return stack == null ? body : stack;
    }

    /** Значение поля: текст от маркера до ближайшего следующего маркера. */
    private String extractField(String body, String marker) {
        int start = body.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int from = start + marker.length();
        int end = body.length();
        for (String other : FIELD_MARKERS) {
            int idx = body.indexOf(other, from);
            if (idx >= 0 && idx < end) {
                end = idx;
            }
        }
        String value = body.substring(from, end)
                .replaceAll("[\\uFE0E\\uFE0F\\u200B-\\u200D]", "")
                .replaceAll("[\\s\\u00A0]+", " ")
                .trim();
        return value.isBlank() ? null : value;
    }

    private int firstMarkerIndex(String body) {
        int cut = body.length();
        for (String marker : FIELD_MARKERS) {
            int idx = body.indexOf(marker);
            if (idx >= 0 && idx < cut) {
                cut = idx;
            }
        }
        return cut;
    }

    private String firstLine(String body) {
        return body.lines().filter(line -> !line.isBlank()).findFirst().orElse(body);
    }

    /** Ссылка на пост в канале. Берём data-post («канал/номер»), а не первую ссылку из текста:
     *  в постах-дайджестах внешние ссылки (hh.ru и т.п.) идут раньше ссылки на сам пост. */
    private String extractPostUrl(Element wrap) {
        Element message = wrap.hasAttr("data-post") ? wrap : wrap.selectFirst("[data-post]");
        if (message != null) {
            String post = message.attr("data-post");
            if (!post.isBlank()) {
                return "https://t.me/" + post;
            }
        }
        Element link = wrap.selectFirst("a.tgme_widget_message_date[href]");
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
