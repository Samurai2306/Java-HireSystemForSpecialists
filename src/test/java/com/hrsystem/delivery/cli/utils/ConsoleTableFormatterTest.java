package com.hrsystem.delivery.cli.utils;

import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.Currency;
import com.hrsystem.domain.enums.VacancySource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleTableFormatterTest {

    private final ConsoleTableFormatter formatter = new ConsoleTableFormatter();

    @Test
    void drawsBordersAndAlignsColumns() {
        String table = formatter.format(
                List.of("ID", "Name"),
                List.of(List.of("1", "Java"), List.of("22", "Go"))
        );
        assertTrue(table.contains("+----+------+"));
        assertTrue(table.contains("| ID | Name |"));
        assertTrue(table.contains("| 22 | Go   |"));
        assertTrue(table.startsWith("+"));
        assertTrue(table.trim().endsWith("+"));
    }

    @Test
    void marksManualVacanciesAsDirectEmployer() {
        assertEquals("[Прямой работодатель]", formatter.sourceMarker(VacancySource.MANUAL));
        assertEquals("[Сайт]", formatter.sourceMarker(VacancySource.WEBSITE));
        assertEquals("[Telegram]", formatter.sourceMarker(VacancySource.TELEGRAM));
    }

    @Test
    void formatsSalaryRange() {
        VacancyEntity vacancy = new VacancyEntity();
        vacancy.setSalaryMin(180_000);
        vacancy.setSalaryMax(240_000);
        vacancy.setCurrency(Currency.RUB);
        String salary = formatter.formatSalary(vacancy);
        assertTrue(salary.contains("180 000"));
        assertTrue(salary.contains("240 000"));
        assertTrue(salary.contains("RUB"));
    }

    @Test
    void emptyTableStillHasFrame() {
        String table = formatter.format(List.of("A", "B"), List.of());
        assertTrue(table.contains("нет данных"));
        assertFalse(table.isBlank());
    }

    @Test
    void visibleLengthIgnoresAnsi() {
        assertEquals(5, formatter.visibleLength(ConsoleTableFormatter.GREEN + "OFFER" + ConsoleTableFormatter.RESET));
    }
}
