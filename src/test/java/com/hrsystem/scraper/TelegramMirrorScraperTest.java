package com.hrsystem.scraper;

import com.hrsystem.domain.entity.ParsingSourceEntity;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.dto.parser.RawParsedVacancyDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты парсера Telegram: сеть подменяется моком HttpFetcher,
 * HTML-страница канала берётся из фикстуры.
 */
class TelegramMirrorScraperTest {

    private static final String POST_PAGE = """
            <html><body>
              <div class="tgme_widget_message_wrap">
                <div class="tgme_widget_message" data-post="java_jobs_ru/1234">
                  <div class="tgme_widget_message_text">
                    #hh 1. Java-разработчик в Big Data 💰 250 000-300 000 руб. 🏢 EPAM Systems 📍 Москва 📋️ Java 17, Spring Boot 🔗 <a href="https://hh.ru/vacancy/1">подробнее</a>
                  </div>
                  <a class="tgme_widget_message_date" href="https://t.me/java_jobs_ru/1234">
                    <time datetime="2026-09-20T10:00:00Z"></time>
                  </a>
                </div>
              </div>
              <div class="tgme_widget_message_wrap">
                <div class="tgme_widget_message_text">Купите наш курс по фотографии со скидкой</div>
              </div>
            </body></html>
            """;

    private final HtmlFetcher httpFetcher = mock(HtmlFetcher.class);
    private final TelegramMirrorScraper scraper = new TelegramMirrorScraper(httpFetcher, new TextCleaner());

    @Test
    @DisplayName("Разбирает пост-дайджест: заголовок, компания, зарплата, город и ссылка")
    void extractsFieldsFromPost() throws IOException {
        when(httpFetcher.fetch(anyString())).thenReturn(page(POST_PAGE, "https://t.me/s/java_jobs_ru"));

        List<RawParsedVacancyDto> items = scraper.scrape(source("https://t.me/s/java_jobs_ru"));

        assertEquals(1, items.size(), "пост без ключевых слов не должен попадать в результат");
        RawParsedVacancyDto dto = items.get(0);
        assertEquals("Java-разработчик в Big Data", dto.getTitle());
        assertEquals("EPAM Systems", dto.getCompanyName());
        assertEquals("250 000-300 000 руб.", dto.getSalaryRaw());
        assertEquals("Москва", dto.getLocation());
        assertEquals("Java 17, Spring Boot", dto.getRequirementsStack());
        assertEquals("https://t.me/java_jobs_ru/1234", dto.getSourceUrl());
        assertEquals(VacancySource.TELEGRAM, dto.getSourceType());
        assertNotNull(dto.getPublishedAt());
    }

    @Test
    @DisplayName("Канал без публичного превью — понятная ошибка, а не 0 карточек")
    void failsWhenChannelHasNoPublicPreview() throws IOException {
        // Telegram редиректит мёртвый канал на t.me/<имя> и отдаёт заглушку с кодом 200
        when(httpFetcher.fetch(anyString())).thenReturn(page(
                "<html><head><title>Telegram: View @java_jobs</title></head><body></body></html>",
                "https://t.me/java_jobs"));

        IOException error = assertThrows(IOException.class,
                () -> scraper.scrape(source("https://t.me/s/java_jobs")));

        assertTrue(error.getMessage().contains("нет публичного веб-превью"), error.getMessage());
    }

    @Test
    @DisplayName("Страница канала открылась, но сообщений нет — тоже ошибка источника")
    void failsWhenPageHasNoMessages() throws IOException {
        when(httpFetcher.fetch(anyString())).thenReturn(page(
                "<html><body><div class=\"tgme_page\">empty</div></body></html>",
                "https://t.me/s/java_jobs_ru"));

        IOException error = assertThrows(IOException.class,
                () -> scraper.scrape(source("https://t.me/s/java_jobs_ru")));

        assertTrue(error.getMessage().contains("сообщений не найдено"), error.getMessage());
    }

    private static Document page(String html, String location) {
        return Jsoup.parse(html, location);
    }

    private static ParsingSourceEntity source(String baseUrl) {
        ParsingSourceEntity source = new ParsingSourceEntity();
        source.setName("Test channel");
        source.setSourceType(VacancySource.TELEGRAM);
        source.setBaseUrl(baseUrl);
        source.setActive(true);
        return source;
    }
}
