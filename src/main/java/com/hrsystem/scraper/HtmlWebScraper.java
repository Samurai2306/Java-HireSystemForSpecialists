package com.hrsystem.scraper;

import com.hrsystem.config.ScraperProperties;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class HtmlWebScraper {

    private final HttpFetcher httpFetcher;
    private final TextCleaner textCleaner;
    private final ScraperProperties properties;

    public HtmlWebScraper(HttpFetcher httpFetcher, TextCleaner textCleaner, ScraperProperties properties) {
        this.httpFetcher = httpFetcher;
        this.textCleaner = textCleaner;
        this.properties = properties;
    }

    public List<RawParsedVacancyDto> scrape(ParsingSourceEntity source) throws IOException {
        Map<String, RawParsedVacancyDto> unique = new LinkedHashMap<>();
        int maxPages = properties.getMaxPages();
        for (int page = 1; page <= maxPages; page++) {
            String pageUrl = withPage(source.getBaseUrl(), page);
            Document document = httpFetcher.fetch(pageUrl);
            Elements cards = selectCards(document);
            if (cards.isEmpty() && page > 1) {
                break;
            }
            for (Element card : cards) {
                RawParsedVacancyDto dto = mapCard(card, source.getBaseUrl());
                if (dto.getTitle() == null || dto.getTitle().isBlank()) {
                    continue;
                }
                String key = dto.getSourceUrl() != null ? dto.getSourceUrl() : dto.getTitle();
                unique.putIfAbsent(key, dto);
            }
            httpFetcher.pause();
        }
        return new ArrayList<>(unique.values());
    }

    public int lastVisitedPagesEstimate() {
        return properties.getMaxPages();
    }

    private Elements selectCards(Document document) {
        Elements habr = document.select(".vacancy-card, article.vacancy-card, .vacancy-list-item");
        if (!habr.isEmpty()) {
            return habr;
        }
        Elements generic = document.select("article, .job-card, .vacancy, .vacancy-item, li.l-vacancy");
        if (!generic.isEmpty()) {
            return generic;
        }
        return document.select("a[href*=vacancy], a[href*=job]");
    }

    private RawParsedVacancyDto mapCard(Element card, String baseUrl) {
        RawParsedVacancyDto dto = new RawParsedVacancyDto();
        String title = firstText(card, ".vacancy-card__title a", ".vacancy-card__title", "h2", "h3", "a");
        dto.setTitle(textCleaner.cleanText(title));
        dto.setCompanyName(nullToDash(textCleaner.cleanText(
                firstText(card, ".vacancy-card__company-title", ".company", ".vacancy-company", ".company-name"))));
        dto.setSalaryRaw(textCleaner.cleanText(firstText(card, ".vacancy-card__salary", ".salary", "[class*=salary]")));
        String description = firstText(card, ".vacancy-card__description", ".description", "p");
        dto.setDescription(textCleaner.cleanText(description.isBlank() ? card.text() : description));
        dto.setRequirementsStack(textCleaner.cleanText(firstText(card, ".vacancy-card__skills", ".skills", ".stack")));
        String href = firstHref(card);
        dto.setSourceUrl(absolutize(baseUrl, href));
        dto.setSourceType(VacancySource.WEBSITE);
        dto.setPublishedAt(Instant.now());
        dto.setLocation(textCleaner.cleanText(firstText(card, ".vacancy-card__meta", ".location")));
        return dto;
    }

    private String firstText(Element root, String... selectors) {
        for (String selector : selectors) {
            Element el = root.selectFirst(selector);
            if (el != null && !el.text().isBlank()) {
                return el.text();
            }
        }
        return "";
    }

    private String firstHref(Element card) {
        Element link = card.selectFirst("a[href]");
        if (link == null && card.hasAttr("href")) {
            return card.attr("href");
        }
        return link == null ? "" : link.attr("href");
    }

    private String absolutize(String baseUrl, String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        try {
            return java.net.URI.create(baseUrl).resolve(href).toString();
        } catch (IllegalArgumentException e) {
            return href;
        }
    }

    private String withPage(String url, int page) {
        if (page <= 1) {
            return url;
        }
        if (url.contains("page=")) {
            return url.replaceAll("page=\\d+", "page=" + page);
        }
        return url + (url.contains("?") ? "&" : "?") + "page=" + page;
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "Не указано" : value;
    }
}
