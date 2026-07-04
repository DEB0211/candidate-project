package com.bis.qa.sftp;

import com.bis.qa.model.BondRecord;
import com.bis.qa.util.IsinGenerator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fluent builder for bond ingestion CSV payloads and file names
 * (PRODUCT.md section 5).
 *
 * <p>Supports both valid and deliberately malformed content so the same helper
 * drives happy-path ingestion and negative validation tests.
 */
public class BondCsvBuilder {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger FILE_SEQ = new AtomicInteger(1);

    private boolean includeHeader = true;
    private final List<BondRecord> rows = new ArrayList<>();
    private final List<String> rawRows = new ArrayList<>();

    public static BondCsvBuilder create() {
        return new BondCsvBuilder();
    }

    public BondCsvBuilder withHeader(boolean include) {
        this.includeHeader = include;
        return this;
    }

    public BondCsvBuilder addRow(BondRecord record) {
        rows.add(record);
        return this;
    }

    /** Adds a raw CSV line verbatim (for malformed-row negative tests). */
    public BondCsvBuilder addRawRow(String rawLine) {
        rawRows.add(rawLine);
        return this;
    }

    /** Builds the full CSV text. */
    public String build() {
        StringBuilder sb = new StringBuilder();
        if (includeHeader) {
            sb.append(BondRecord.HEADER).append('\n');
        }
        for (BondRecord r : rows) {
            sb.append(r.toCsvRow()).append('\n');
        }
        for (String raw : rawRows) {
            sb.append(raw).append('\n');
        }
        return sb.toString();
    }

    /** Convention-compliant unique file name: {@code BONDS_YYYYMMDD_NNN.csv}. */
    public static String fileName() {
        return fileName(LocalDate.now());
    }

    public static String fileName(LocalDate date) {
        int seq = FILE_SEQ.getAndIncrement();
        return String.format("BONDS_%s_%03d.csv", date.format(FILE_DATE), seq);
    }

    // ----- Factory helpers for common records ------------------------------------

    /**
     * A fully valid bond whose book opens on {@code bookOpen}. Coupon rate and
     * face value default to the canonical worked-example values.
     */
    public static BondRecord validBond(LocalDate bookOpen, LocalDate bookClose, LocalDate maturity) {
        return new BondRecord()
                .isin(IsinGenerator.unique())
                .issuerName("Test Issuer")
                .bondName("Test 2026 Notes")
                .currency("MYR")
                .faceValue("1000.00")
                .couponRate("0.0005")
                .maturityDate(maturity)
                .totalSize(100_000)
                .bookOpenDate(bookOpen)
                .bookCloseDate(bookClose);
    }
}
