package com.hrsystem.dto.response;

import com.hrsystem.domain.enums.Currency;
import com.hrsystem.domain.enums.EmploymentType;
import com.hrsystem.domain.enums.VacancySource;

public class VacancyDetailsDto {
    private Long id;
    private String title;
    private String companyName;
    private Integer salaryMin;
    private Integer salaryMax;
    private Currency currency;
    private String salaryFormatted;
    private String description;
    private String requirementsStack;
    private String location;
    private EmploymentType employmentType;
    private VacancySource sourceType;
    private String sourceFormatted;
    private String sourceUrl;
    private String publishedDateFormatted;

    public VacancyDetailsDto() {
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

    public Integer getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(Integer salaryMin) {
        this.salaryMin = salaryMin;
    }

    public Integer getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(Integer salaryMax) {
        this.salaryMax = salaryMax;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getSalaryFormatted() {
        return salaryFormatted;
    }

    public void setSalaryFormatted(String salaryFormatted) {
        this.salaryFormatted = salaryFormatted;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public VacancySource getSourceType() {
        return sourceType;
    }

    public void setSourceType(VacancySource sourceType) {
        this.sourceType = sourceType;
        this.sourceFormatted = VacancySummaryDto.formatSource(sourceType);
    }

    public String getSourceFormatted() {
        return sourceFormatted;
    }

    public void setSourceFormatted(String sourceFormatted) {
        this.sourceFormatted = sourceFormatted;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getPublishedDateFormatted() {
        return publishedDateFormatted;
    }

    public void setPublishedDateFormatted(String publishedDateFormatted) {
        this.publishedDateFormatted = publishedDateFormatted;
    }
}
