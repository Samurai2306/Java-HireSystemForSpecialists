package com.hrsystem.dto.parser;

import com.hrsystem.domain.enums.VacancySource;

import java.time.Instant;

public class RawParsedVacancyDto {
    private String title;
    private String companyName;
    private String salaryRaw;
    private String description;
    private String requirementsStack;
    private String sourceUrl;
    private Instant publishedAt;
    private VacancySource sourceType;
    private String location;

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

    public String getSalaryRaw() {
        return salaryRaw;
    }

    public void setSalaryRaw(String salaryRaw) {
        this.salaryRaw = salaryRaw;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequirementsStack() {
        return requirementsStack;
    }

    public void setRequirementsStack(String requirementsStack) {
        this.requirementsStack = requirementsStack;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public VacancySource getSourceType() {
        return sourceType;
    }

    public void setSourceType(VacancySource sourceType) {
        this.sourceType = sourceType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
