package com.hrsystem.domain.state;

import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ApplicationStateMachine {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = new EnumMap<>(ApplicationStatus.class);

    static {
        allow(ApplicationStatus.APPLIED, ApplicationStatus.REVIEWING, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN);
        allow(ApplicationStatus.REVIEWING, ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN);
        allow(ApplicationStatus.OFFER);
        allow(ApplicationStatus.REJECTED);
        allow(ApplicationStatus.WITHDRAWN);
    }

    public boolean canTransition(ApplicationStatus from, ApplicationStatus to) {
        return from != null && to != null && ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public void validate(ApplicationStatus from, ApplicationStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(
                    "Недопустимый переход статуса отклика: " + from + " → " + to
                            + ". Терминальные статусы OFFER, REJECTED и WITHDRAWN нельзя изменить.");
        }
    }

    private static void allow(ApplicationStatus from, ApplicationStatus... to) {
        ALLOWED.put(from, to.length == 0 ? EnumSet.noneOf(ApplicationStatus.class) : EnumSet.copyOf(List.of(to)));
    }
}
