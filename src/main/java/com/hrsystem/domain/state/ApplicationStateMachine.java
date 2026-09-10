package com.hrsystem.domain.state;

import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.exception.InvalidStateTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class ApplicationStateMachine {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = new EnumMap<>(ApplicationStatus.class);

    static {
        ALLOWED.put(ApplicationStatus.APPLIED, EnumSet.of(
                ApplicationStatus.REVIEWING,
                ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN
        ));
        ALLOWED.put(ApplicationStatus.REVIEWING, EnumSet.of(
                ApplicationStatus.OFFER,
                ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN
        ));
        ALLOWED.put(ApplicationStatus.OFFER, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED.put(ApplicationStatus.REJECTED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED.put(ApplicationStatus.WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class));
    }

    public boolean canTransition(ApplicationStatus from, ApplicationStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<ApplicationStatus> targets = ALLOWED.get(from);
        return targets != null && targets.contains(to);
    }

    public void validate(ApplicationStatus from, ApplicationStatus to) {
        if (canTransition(from, to)) {
            return;
        }
        throw new InvalidStateTransitionException(
                "Недопустимый переход статуса отклика: " + from + " → " + to
                        + ". Терминальные статусы OFFER, REJECTED и WITHDRAWN нельзя изменить."
        );
    }
}
