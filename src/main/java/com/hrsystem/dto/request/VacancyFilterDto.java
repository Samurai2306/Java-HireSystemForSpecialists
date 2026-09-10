package com.hrsystem.dto.request;

import com.hrsystem.domain.enums.VacancySource;

public class VacancyFilterDto {
    private String keyword;
    private Integer salaryMin;
    private VacancySource sourceType;
    private int page = 0;
    private int pageSize = 10;

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

    public VacancySource getSourceType() {
        return sourceType;
    }

    public void setSourceType(VacancySource sourceType) {
        this.sourceType = sourceType;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(page, 0);
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
