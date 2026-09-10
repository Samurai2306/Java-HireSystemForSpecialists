package com.hrsystem.scraper;

import com.hrsystem.domain.entity.ParsingLogEntity;
import com.hrsystem.domain.entity.ParsingSourceEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.domain.enums.VacancyStatus;
import com.hrsystem.dto.parser.RawParsedVacancyDto;
import com.hrsystem.dto.response.ParsingReportDto;
import com.hrsystem.repository.ParsingLogRepository;
import com.hrsystem.repository.ParsingSourceRepository;
import com.hrsystem.repository.VacancyRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
public class ScraperCoordinatorService {

    private final ParsingSourceRepository parsingSourceRepository;
    private final ParsingLogRepository parsingLogRepository;
    private final VacancyRepository vacancyRepository;
    private final HtmlWebScraper htmlWebScraper;
    private final TelegramMirrorScraper telegramMirrorScraper;
    private final TextCleaner textCleaner;
    private final SalaryParser salaryParser;
    private final ContentHasher contentHasher;

    public ScraperCoordinatorService(ParsingSourceRepository parsingSourceRepository,
                                     ParsingLogRepository parsingLogRepository,
                                     VacancyRepository vacancyRepository,
                                     HtmlWebScraper htmlWebScraper,
                                     TelegramMirrorScraper telegramMirrorScraper,
                                     TextCleaner textCleaner,
                                     SalaryParser salaryParser,
                                     ContentHasher contentHasher) {
        this.parsingSourceRepository = parsingSourceRepository;
        this.parsingLogRepository = parsingLogRepository;
        this.vacancyRepository = vacancyRepository;
        this.htmlWebScraper = htmlWebScraper;
        this.telegramMirrorScraper = telegramMirrorScraper;
        this.textCleaner = textCleaner;
        this.salaryParser = salaryParser;
        this.contentHasher = contentHasher;
    }

    public ParsingReportDto runFullScraping() {
        return runFullScraping(msg -> {
        });
    }

    public ParsingReportDto runFullScraping(Consumer<String> liveLog) {
        return runByTypes(List.of(VacancySource.WEBSITE, VacancySource.TELEGRAM), liveLog);
    }

    public ParsingReportDto runWebsiteScraping() {
        return runWebsiteScraping(msg -> {
        });
    }

    public ParsingReportDto runWebsiteScraping(Consumer<String> liveLog) {
        return runByTypes(List.of(VacancySource.WEBSITE), liveLog);
    }

    public ParsingReportDto runTelegramScraping() {
        return runTelegramScraping(msg -> {
        });
    }

    public ParsingReportDto runTelegramScraping(Consumer<String> liveLog) {
        return runByTypes(List.of(VacancySource.TELEGRAM), liveLog);
    }

    private ParsingReportDto runByTypes(List<VacancySource> types, Consumer<String> liveLog) {
        ParsingReportDto total = new ParsingReportDto();
        total.setStartedAt(Instant.now());
        total.setSourceFilter(types.size() == 1 ? types.get(0) : null);
        List<ParsingSourceEntity> sources = new ArrayList<>();
        for (VacancySource type : types) {
            sources.addAll(parsingSourceRepository.findByActiveTrueAndSourceType(type));
        }
        liveLog.accept("Активных источников: " + sources.size());
        for (ParsingSourceEntity source : sources) {
            liveLog.accept("→ Источник [" + source.getSourceType() + "] " + source.getName() + " (" + source.getBaseUrl() + ")");
            ParsingReportDto one = scrapeSource(source, liveLog);
            total.add(one);
        }
        total.setFinishedAt(Instant.now());
        total.setSummary(String.format(
                "Обойдено источников: %d, найдено: %d, добавлено новых: %d, дубликатов: %d, ошибок: %d",
                sources.size(), total.getItemsFound(), total.getItemsSaved(),
                total.getDuplicatesSkipped(), total.getFailedSources()
        ));
        liveLog.accept(total.getSummary());
        return total;
    }

    public ParsingReportDto scrapeSource(ParsingSourceEntity source, Consumer<String> liveLog) {
        ParsingReportDto report = new ParsingReportDto();
        Instant started = Instant.now();
        ParsingLogEntity log = new ParsingLogEntity();
        log.setSource(source);
        log.setStartedAt(started);
        try {
            List<RawParsedVacancyDto> rawItems = source.getSourceType() == VacancySource.TELEGRAM
                    ? telegramMirrorScraper.scrape(source)
                    : htmlWebScraper.scrape(source);
            if (source.getSourceType() == VacancySource.WEBSITE) {
                report.setPagesVisited(htmlWebScraper.lastVisitedPagesEstimate());
            } else {
                report.setPagesVisited(1);
            }
            report.setItemsFound(rawItems.size());
            liveLog.accept("   найдено карточек: " + rawItems.size());
            int saved = 0;
            int duplicates = 0;
            for (RawParsedVacancyDto raw : rawItems) {
                if (persistIfUnique(raw, source)) {
                    saved++;
                } else {
                    duplicates++;
                }
            }
            report.setItemsSaved(saved);
            report.setDuplicatesSkipped(duplicates);
            log.setItemsFound(rawItems.size());
            log.setItemsSaved(saved);
            log.setDuplicatesSkipped(duplicates);
            log.setStatus("SUCCESS");
            source.setLastScrapedAt(Instant.now());
            parsingSourceRepository.save(source);
            liveLog.accept("   сохранено: " + saved + ", дубликатов: " + duplicates);
        } catch (Exception ex) {
            report.setFailedSources(1);
            log.setStatus("FAILED");
            log.setErrorMessage(ex.getMessage());
            liveLog.accept("   ошибка: " + ex.getMessage());
        }
        log.setFinishedAt(Instant.now());
        parsingLogRepository.save(log);
        return report;
    }

    private boolean persistIfUnique(RawParsedVacancyDto raw, ParsingSourceEntity source) {
        String title = textCleaner.cleanText(raw.getTitle());
        String company = textCleaner.cleanText(raw.getCompanyName());
        String description = textCleaner.cleanText(raw.getDescription());
        if (title.isBlank() || description.isBlank()) {
            return false;
        }
        String hash = contentHasher.fingerprint(title, company, description);
        if (vacancyRepository.existsByContentHash(hash)) {
            return false;
        }
        if (raw.getSourceUrl() != null && vacancyRepository.existsBySourceUrl(raw.getSourceUrl())) {
            return false;
        }
        SalaryParser.ParsedSalary salary = salaryParser.parse(raw.getSalaryRaw() == null ? description : raw.getSalaryRaw());
        VacancyEntity vacancy = new VacancyEntity();
        vacancy.setTitle(truncate(title, 255));
        vacancy.setCompanyName(truncate(company.isBlank() ? "Не указано" : company, 255));
        vacancy.setDescription(description);
        vacancy.setRequirementsStack(textCleaner.cleanText(raw.getRequirementsStack()));
        vacancy.setSalaryMin(salary.min());
        vacancy.setSalaryMax(salary.max());
        vacancy.setCurrency(salary.currency());
        vacancy.setSourceType(source.getSourceType());
        vacancy.setSource(source);
        vacancy.setSourceUrl(raw.getSourceUrl());
        vacancy.setContentHash(hash);
        vacancy.setParsed(true);
        vacancy.setStatus(VacancyStatus.ACTIVE);
        vacancy.setLocation(raw.getLocation() == null || raw.getLocation().isBlank() ? "Не указано" : raw.getLocation());
        if (raw.getPublishedAt() != null) {
            vacancy.setPublishedAt(raw.getPublishedAt());
        }
        vacancyRepository.save(vacancy);
        return true;
    }

    private String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
