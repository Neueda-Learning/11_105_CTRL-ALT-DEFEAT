package com.finance.PaymentProcessing.util;

import java.security.SecureRandom;

/**
 * Generates unique 9-digit numeric identifiers used as primary keys
 * throughout the application, in place of {@code java.util.UUID}.
 *
 * The generated value is always exactly 9 digits (100000000-999999999),
 * so the leading digit is never zero.
 */
public final class UniqueIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long MIN_VALUE = 100_000_000L;
    private static final long MAX_VALUE = 999_999_999L;

    private UniqueIdGenerator() {
    }

    public static String generate() {
        long value = MIN_VALUE + (long) (RANDOM.nextDouble() * (MAX_VALUE - MIN_VALUE + 1));
        return Long.toString(value);
    }
}
