package com.hrsystem.dto.request;

import com.hrsystem.domain.enums.ApplicationStatus;

public class UpdateStatusDto {
    private Long applicationId;
    private ApplicationStatus targetStatus;
    private String comment;

    public UpdateStatusDto() {
    }

    public UpdateStatusDto(Long applicationId, ApplicationStatus targetStatus, String comment) {
        this.applicationId = applicationId;
        this.targetStatus = targetStatus;
        this.comment = comment;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public ApplicationStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(ApplicationStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
