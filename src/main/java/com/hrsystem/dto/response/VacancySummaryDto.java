package com.hrsystem.dto.response;

import com.hrsystem.domain.enums.VacancySource;

public class VacancySummaryDto {
    private Long id;
    private String title;
    private String companyName;
    private String salaryFormatted;
    private VacancySource sourceType;
    private String sourceFormatted;
    private String publishedDateFormatted;

    public VacancySummaryDto() {
    }

    public VacancySummaryDto(Long id, String title, String companyName, String salaryFormatted,
                             VacancySource sourceType, String publishedDateFormatted) {
        this.id = id;
        this.title = title;
        this.companyName = companyName;
        this.salaryFormatted = salaryFormatted;
        this.sourceType = sourceType;
        this.publishedDateFormatted = publishedDateFormatted;
        this.sourceFormatted = formatSource(sourceType);
    }

    public static String formatSource(VacancySource source) {
        if (source == null) return "[Не указан]";
        switch (source) {
            case WEBSITE:
                return "[Сайт]";
            case TELEGRAM:
                return "[Telegram]";
            case MANUAL:
                return "[Работодатель]";
            default:
                return "[" + source.name() + "]";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getSalaryFormatted() {
        return salaryFormatted;
    }

    public void setSalaryFormatted(String salaryFormatted) {
        this.salaryFormatted = salaryFormatted;
    }

    public VacancySource getSourceType() {
        return sourceType;
    }

    public void setSourceType(VacancySource sourceType) {
        this.sourceType = sourceType;
        this.sourceFormatted = formatSource(sourceType);
    }

    public String getSourceFormatted() {
        return sourceFormatted;
    }

    public void setSourceFormatted(String sourceFormatted) {
        this.sourceFormatted = sourceFormatted;
    }

    public String getPublishedDateFormatted() {
        return publishedDateFormatted;
    }

    public void setPublishedDateFormatted(String publishedDateFormatted) {
        this.publishedDateFormatted = publishedDateFormatted;
    }
}
