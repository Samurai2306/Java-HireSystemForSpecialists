package com.hrsystem.dto.response;

import com.hrsystem.domain.enums.ApplicationStatus;

public class ApplicationDto {
    private Long id;
    private Long vacancyId;
    private String vacancyTitle;
    private String companyName;
    private Long candidateId;
    private String candidateFullName;
    private String coverLetter;
    private ApplicationStatus status;
    private String statusComment;
    private String appliedAtFormatted;
    private String updatedAtFormatted;

    public ApplicationDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVacancyId() {
        return vacancyId;
    }

    public void setVacancyId(Long vacancyId) {
        this.vacancyId = vacancyId;
    }

    public String getVacancyTitle() {
        return vacancyTitle;
    }

    public void setVacancyTitle(String vacancyTitle) {
        this.vacancyTitle = vacancyTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateFullName() {
        return candidateFullName;
    }

    public void setCandidateFullName(String candidateFullName) {
        this.candidateFullName = candidateFullName;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getStatusComment() {
        return statusComment;
    }

    public void setStatusComment(String statusComment) {
        this.statusComment = statusComment;
    }

    public String getAppliedAtFormatted() {
        return appliedAtFormatted;
    }

    public void setAppliedAtFormatted(String appliedAtFormatted) {
        this.appliedAtFormatted = appliedAtFormatted;
    }

    public String getUpdatedAtFormatted() {
        return updatedAtFormatted;
    }

    public void setUpdatedAtFormatted(String updatedAtFormatted) {
        this.updatedAtFormatted = updatedAtFormatted;
    }
}
