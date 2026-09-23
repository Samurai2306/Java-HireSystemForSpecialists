package com.hrsystem.domain.state;

import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStateMachine {

    public boolean canTransition(ApplicationStatus from, ApplicationStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == ApplicationStatus.APPLIED) {
            return to == ApplicationStatus.REVIEWING
                    || to == ApplicationStatus.REJECTED
                    || to == ApplicationStatus.WITHDRAWN;
        }
        if (from == ApplicationStatus.REVIEWING) {
            return to == ApplicationStatus.OFFER
                    || to == ApplicationStatus.REJECTED
                    || to == ApplicationStatus.WITHDRAWN;
        }
        return false;
    }

    public void validate(ApplicationStatus from, ApplicationStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(
                    "Недопустимый переход статуса отклика: " + from + " → " + to
                            + ". Терминальные статусы OFFER, REJECTED и WITHDRAWN нельзя изменить.");
        }
    }
}
