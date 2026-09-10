package com.hrsystem.service;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.domain.enums.UserRole;

public interface ApplicationService {

    ApplicationEntity changeStatus(Long applicationId, ApplicationStatus targetStatus, String comment,
                                   Long actorUserId, UserRole actorRole);

    ApplicationEntity apply(Long candidateProfileId, Long vacancyId, String coverLetter);

    ApplicationEntity withdraw(Long candidateProfileId, Long applicationId);
}
