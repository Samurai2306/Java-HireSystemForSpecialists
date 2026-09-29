package com.hrsystem.dto.request;

public class ApplyVacancyDto {
    private Long vacancyId;
    private String coverLetter;
    private boolean useProfileDefault;

    public ApplyVacancyDto() {
    }

    public ApplyVacancyDto(Long vacancyId, String coverLetter) {
        this.vacancyId = vacancyId;
        this.coverLetter = coverLetter;
        this.useProfileDefault = false;
    }

    public ApplyVacancyDto(Long vacancyId, String coverLetter, boolean useProfileDefault) {
        this.vacancyId = vacancyId;
        this.coverLetter = coverLetter;
        this.useProfileDefault = useProfileDefault;
    }

    public Long getVacancyId() {
        return vacancyId;
    }

    public void setVacancyId(Long vacancyId) {
        this.vacancyId = vacancyId;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public boolean isUseProfileDefault() {
        return useProfileDefault;
    }

    public void setUseProfileDefault(boolean useProfileDefault) {
        this.useProfileDefault = useProfileDefault;
    }
}
