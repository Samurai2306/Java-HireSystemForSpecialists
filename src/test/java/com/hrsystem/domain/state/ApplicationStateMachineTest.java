package com.hrsystem.domain.state;

import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationStateMachineTest {

    private final ApplicationStateMachine machine = new ApplicationStateMachine();

    @ParameterizedTest
    @CsvSource({
            "APPLIED,REVIEWING",
            "APPLIED,REJECTED",
            "APPLIED,WITHDRAWN",
            "REVIEWING,OFFER",
            "REVIEWING,REJECTED",
            "REVIEWING,WITHDRAWN"
    })
    void allowsValidTransitions(ApplicationStatus from, ApplicationStatus to) {
        assertTrue(machine.canTransition(from, to));
        assertDoesNotThrow(() -> machine.validate(from, to));
    }

    @ParameterizedTest
    @CsvSource({
            "APPLIED,OFFER",
            "APPLIED,APPLIED",
            "REVIEWING,APPLIED",
            "REVIEWING,REVIEWING",
            "OFFER,APPLIED",
            "OFFER,REVIEWING",
            "OFFER,REJECTED",
            "REJECTED,OFFER",
            "REJECTED,APPLIED",
            "WITHDRAWN,REVIEWING",
            "WITHDRAWN,OFFER"
    })
    void blocksInvalidAndTerminalTransitions(ApplicationStatus from, ApplicationStatus to) {
        assertFalse(machine.canTransition(from, to));
        assertThrows(InvalidStateTransitionException.class, () -> machine.validate(from, to));
    }

    @Test
    void rejectsNullStatuses() {
        assertFalse(machine.canTransition(null, ApplicationStatus.REVIEWING));
        assertThrows(InvalidStateTransitionException.class,
                () -> machine.validate(ApplicationStatus.APPLIED, null));
    }
}
