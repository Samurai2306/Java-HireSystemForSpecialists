package com.hrsystem.delivery.cli.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidatorTest {

    @Test
    void retriesUntilIntegerInRange() {
        InputValidator validator = validator("abc\n99\n3\n");
        assertEquals(3, validator.readIntInRange("> ", 1, 5));
    }

    @Test
    void requiredStringRejectsBlank() {
        InputValidator validator = validator("\n  \nJava\n");
        assertEquals("Java", validator.readRequiredString("> "));
    }

    @Test
    void validatesEmailAndUrl() {
        InputValidator validator = validator("bad\nuser@mail.com\nftp://x\nhttps://example.com\n");
        assertEquals("user@mail.com", validator.readEmail("email > "));
        assertEquals("https://example.com", validator.readUrl("url > "));
        assertTrue(validator.isEmail("a@b.co"));
        assertFalse(validator.isEmail("not-an-email"));
        assertTrue(validator.isUrl("http://localhost"));
        assertFalse(validator.isUrl("example.com"));
    }

    @Test
    void optionalIntAllowsEmpty() {
        InputValidator validator = validator("\n12\n");
        org.junit.jupiter.api.Assertions.assertNull(validator.readOptionalInt("> "));
        assertEquals(12, validator.readOptionalInt("> "));
    }

    private InputValidator validator(String input) {
        Scanner scanner = new Scanner(input);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        return new InputValidator(scanner, new PrintStream(buffer, true, StandardCharsets.UTF_8));
    }
}
