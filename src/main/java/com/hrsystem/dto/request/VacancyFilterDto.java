package com.hrsystem.dto.request;

import com.hrsystem.domain.enums.VacancySource;

public class VacancyFilterDto {
    private String keyword;
    private Integer salaryMin;
    private VacancySource source = VacancySource.ALL;
    private int pageNumber = 1;
    private int pageSize = 10;

    public VacancyFilterDto() {
    }

    public VacancyFilterDto(String keyword, Integer salaryMin, VacancySource source, int pageNumber, int pageSize) {
        this.keyword = keyword;
        this.salaryMin = salaryMin;
        this.source = (source != null) ? source : VacancySource.ALL;
        this.pageNumber = Math.max(1, pageNumber);
        this.pageSize = (pageSize > 0) ? pageSize : 10;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(Integer salaryMin) {
        this.salaryMin = salaryMin;
    }

    public VacancySource getSource() {
        return source;
    }

    public void setSource(VacancySource source) {
        this.source = (source != null) ? source : VacancySource.ALL;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = Math.max(1, pageNumber);
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = (pageSize > 0) ? pageSize : 10;
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }

    public boolean hasSalaryMin() {
        return salaryMin != null && salaryMin > 0;
    }

    public boolean hasSourceFilter() {
        return source != null && source != VacancySource.ALL;
    }
}
