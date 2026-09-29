package com.hrsystem.scraper;

import com.hrsystem.domain.enums.Currency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SalaryParserAndHasherTest {

    private final SalaryParser parser = new SalaryParser();
    private final ContentHasher hasher = new ContentHasher();

    @Test
    void parsesRussianRange() {
        SalaryParser.ParsedSalary salary = parser.parse("от 150 000 до 250 000 руб.");
        assertEquals(150_000, salary.min());
        assertEquals(250_000, salary.max());
        assertEquals(Currency.RUB, salary.currency());
    }

    @Test
    void parsesUsdShortcut() {
        SalaryParser.ParsedSalary salary = parser.parse("$3000 - $5000");
        assertEquals(3000, salary.min());
        assertEquals(5000, salary.max());
        assertEquals(Currency.USD, salary.currency());
    }

    @Test
    void fingerprintIsStableAndSensitive() {
        String first = hasher.fingerprint("Java Dev", "TechNova", "Spring Boot");
        String second = hasher.fingerprint("java dev", "technova", "spring boot!!!");
        assertEquals(first, second);
        assertEquals(64, first.length());
        assertNotEquals(first, hasher.fingerprint("Java Dev", "Other", "Spring Boot"));
        assertNotNull(first);
    }
}
