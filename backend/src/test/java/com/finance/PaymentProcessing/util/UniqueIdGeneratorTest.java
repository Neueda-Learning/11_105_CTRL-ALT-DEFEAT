package com.finance.PaymentProcessing.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UniqueIdGeneratorTest {

    @Test
    void generate_returnsNineDigitNumericString() {
        String id = UniqueIdGenerator.generate();

        assertEquals(9, id.length());
        assertTrue(id.matches("\\d{9}"));
        assertTrue(id.charAt(0) != '0');
    }

    @Test
    void generate_valueWithinExpectedRange() {
        long value = Long.parseLong(UniqueIdGenerator.generate());

        assertTrue(value >= 100_000_000L && value <= 999_999_999L);
    }

    @Test
    void generate_producesDistinctValuesAcrossManyCalls() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            generated.add(UniqueIdGenerator.generate());
        }

        assertTrue(generated.size() > 90);
    }
}
