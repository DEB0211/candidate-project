package com.bis.qa.util;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique, well-formed ISINs for test bonds.
 *
 * <p>PRODUCT.md section 5 requires ISINs to be alphanumeric and exactly 12
 * characters and unique across all bonds. To keep runs independent we combine a
 * fixed prefix, a random block and a monotonically increasing counter, always
 * trimmed/padded to exactly 12 characters.
 */
public final class IsinGenerator {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private IsinGenerator() {
    }

    /** A unique valid 12-character alphanumeric ISIN, e.g. {@code MY7QK3D00042}. */
    public static String unique() {
        String prefix = "MY"; // Malaysia-style prefix; only the format matters here.
        StringBuilder sb = new StringBuilder(prefix);
        // 6 random alphanumeric chars.
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHANUM[ThreadLocalRandom.current().nextInt(ALPHANUM.length)]);
        }
        // 4-digit zero-padded counter for guaranteed uniqueness within a run.
        sb.append(String.format("%04d", COUNTER.incrementAndGet() % 10000));
        return sb.substring(0, 12);
    }

    /** A valid ISIN of an arbitrary length, for length-validation negative tests. */
    public static String ofLength(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUM[ThreadLocalRandom.current().nextInt(ALPHANUM.length)]);
        }
        return sb.toString();
    }
}
