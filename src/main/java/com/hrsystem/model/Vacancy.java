package com.hrsystem.model;

import java.math.BigDecimal;

public class Vacancy {
    private Long id;
    private String title;
    private String companyName;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private VacancyStatus status;

    public Vacancy(Long id, String title, String companyName, BigDecimal salaryMin, BigDecimal salaryMax, String statusStr) {
        this.id = id;
        this.title = title;
        this.companyName = companyName;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        // строку из базы переводим в enum, если не подошла — ставим ACTIVE
        try {
            this.status = VacancyStatus.valueOf(statusStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            this.status = VacancyStatus.ACTIVE;
        }
    }

    public Vacancy(String title, String companyName, BigDecimal salaryMin, BigDecimal salaryMax, VacancyStatus status) {
        this.id = null;
        this.title = title;
        this.companyName = companyName;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public BigDecimal getSalaryMin() { return salaryMin; }
    public void setSalaryMin(BigDecimal salaryMin) { this.salaryMin = salaryMin; }
    public BigDecimal getSalaryMax() { return salaryMax; }
    public void setSalaryMax(BigDecimal salaryMax) { this.salaryMax = salaryMax; }
    public VacancyStatus getStatus() { return status; }
    public void setStatus(VacancyStatus status) { this.status = status; }

    // красивый вывод в консоль
    public void printFormatted() {
        System.out.printf("[%4d] %-20s | %-15s | От: %-10s | До: %-10s | Статус: %s%n",
                id, title, companyName, 
                salaryMin != null ? salaryMin.toString() : "---", 
                salaryMax != null ? salaryMax.toString() : "---", 
                status);
    }
}
