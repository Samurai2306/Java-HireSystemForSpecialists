package com.hrsystem.dto.response;

import com.hrsystem.domain.enums.VacancySource;

import java.time.Instant;

public class ParsingReportDto {
    private int pagesVisited;
    private int itemsFound;
    private int itemsSaved;
    private int duplicatesSkipped;
    private int failedSources;
    private Instant startedAt;
    private Instant finishedAt;
    private String summary;
    private VacancySource sourceFilter;

    public int getPagesVisited() {
        return pagesVisited;
    }

    public void setPagesVisited(int pagesVisited) {
        this.pagesVisited = pagesVisited;
    }

    public int getItemsFound() {
        return itemsFound;
    }

    public void setItemsFound(int itemsFound) {
        this.itemsFound = itemsFound;
    }

    public int getItemsSaved() {
        return itemsSaved;
    }

    public void setItemsSaved(int itemsSaved) {
        this.itemsSaved = itemsSaved;
    }

    public int getDuplicatesSkipped() {
        return duplicatesSkipped;
    }

    public void setDuplicatesSkipped(int duplicatesSkipped) {
        this.duplicatesSkipped = duplicatesSkipped;
    }

    public int getFailedSources() {
        return failedSources;
    }

    public void setFailedSources(int failedSources) {
        this.failedSources = failedSources;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public VacancySource getSourceFilter() {
        return sourceFilter;
    }

    public void setSourceFilter(VacancySource sourceFilter) {
        this.sourceFilter = sourceFilter;
    }

    public void add(ParsingReportDto other) {
        this.pagesVisited += other.pagesVisited;
        this.itemsFound += other.itemsFound;
        this.itemsSaved += other.itemsSaved;
        this.duplicatesSkipped += other.duplicatesSkipped;
        this.failedSources += other.failedSources;
    }
}
