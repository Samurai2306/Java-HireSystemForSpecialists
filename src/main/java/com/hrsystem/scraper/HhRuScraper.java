package com.hrsystem.scraper;

import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.dto.parser.RawParsedVacancyDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class HhRuScraper {

    private static final String BASE_URL = "https://hh.ru/search/vacancy";
    private static final int TIMEOUT_MS = 10_000;

    private static final Map<String, String> DEFAULT_HEADERS = Map.of(
            "User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
            "Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7",
            "Sec-Ch-Ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"",
            "Sec-Ch-Ua-Platform", "\"macOS\"",
            "Upgrade-Insecure-Requests", "1",
            "Referer", "https://www.google.com/"
    );

    public List<RawParsedVacancyDto> parseHh(String query, int page) {
        List<RawParsedVacancyDto> vacancies = new ArrayList<>(25);

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = BASE_URL + "?text=" + encodedQuery + "&area=1&page=" + page;
        System.out.println("Подключаемся к hh.ru: " + url);

        try {
            Document doc = Jsoup.connect(url)
                    .headers(DEFAULT_HEADERS)
                    .timeout(TIMEOUT_MS)
                    .get();

            System.out.println("Страница успешно скачана! Заголовок: " + doc.title());

            Elements cards = doc.select("[data-qa=vacancy-serp__vacancy]");
            if (cards.isEmpty()) {
                cards = doc.select(".vacancy-card--H8woOUjhPfP8gluKg2hg, .serp-item");
            }

            System.out.println("Найдено карточек вакансий: " + cards.size());

            for (Element card : cards) {
                Element titleElement = card.selectFirst("[data-qa=serp-item__title]");
                String title = (titleElement != null) ? titleElement.text() : "Без названия";

                String link = (titleElement != null) ? titleElement.attr("href") : "";
                if (link.contains("?")) {
                    link = link.substring(0, link.indexOf("?"));
                }

                Element employerElement = card.selectFirst("[data-qa=vacancy-serp__vacancy-employer]");
                if (employerElement == null) {
                    employerElement = card.selectFirst("[data-qa=vacancy-serp__vacancy_employer]");
                }
                String employer = (employerElement != null) ? employerElement.text() : "Компания не указана";

                Element salaryElement = card.selectFirst("[data-qa=vacancy-serp__vacancy-compensation]");
                String salary = (salaryElement != null) ? salaryElement.text() : "Не указана";

                Element snippetElement = card.selectFirst("[data-qa=vacancy-serp__vacancy_snippet_requirement]");
                String description = (snippetElement != null) ? snippetElement.text() : "Требования в описании";

                RawParsedVacancyDto dto = new RawParsedVacancyDto();
                dto.setTitle(title);
                dto.setCompanyName(employer);
                dto.setSalaryRaw(salary);
                dto.setDescription(description);
                dto.setSourceUrl(link);
                dto.setSourceType(VacancySource.WEBSITE);
                dto.setLocation("Москва");
                dto.setPublishedAt(Instant.now());

                vacancies.add(dto);
            }

        } catch (IOException e) {
            System.err.println("Ошибка при запросе к hh.ru: " + e.getMessage());
        }

        return vacancies;
    }

    public static void main(String[] args) {
        HhRuScraper scraper = new HhRuScraper();
        System.out.println("Запуск боевого парсинга вакансий с hh.ru (Москва)...");
        List<RawParsedVacancyDto> result = scraper.parseHh("java", 0);
        System.out.println("Итого успешно спарсено: " + result.size() + " вакансий.");
    }
}
