package com.hrsystem.dto.request;

import com.hrsystem.domain.enums.Currency;
import com.hrsystem.domain.enums.EmploymentType;

public class CreateVacancyDto {
    private String title;
    private Integer salaryMin;
    private Integer salaryMax;
    private Currency currency;
    private String requirementsStack;
    private String description;
    private EmploymentType employmentType;
    private String location;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getRequirementsStack() {
        return requirementsStack;
    }

    public void setRequirementsStack(String requirementsStack) {
        this.requirementsStack = requirementsStack;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
