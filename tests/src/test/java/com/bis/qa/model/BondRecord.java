package com.bis.qa.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A bond definition as it appears in an ingestion CSV row (PRODUCT.md section 5).
 *
 * <p>Fields are intentionally stored as the exact strings that go into the CSV so
 * that negative-path tests can inject malformed values (e.g. {@code faceValue="not_a_number"})
 * while positive-path tests use the strongly typed factory helpers.
 */
public class BondRecord {

    public String isin;
    public String issuerName;
    public String bondName;
    public String currency;
    public String faceValue;
    public String couponRate;
    public String maturityDate;
    public String totalSize;
    public String bookOpenDate;
    public String bookCloseDate;

    public BondRecord() {
    }

    /** CSV header exactly as required by PRODUCT.md section 5. */
    public static final String HEADER =
            "isin,issuerName,bondName,currency,faceValue,couponRate,maturityDate,totalSize,bookOpenDate,bookCloseDate";

    /** Produces the CSV line for this record in the canonical column order. */
    public String toCsvRow() {
        return String.join(",",
                nullToEmpty(isin),
                nullToEmpty(issuerName),
                nullToEmpty(bondName),
                nullToEmpty(currency),
                nullToEmpty(faceValue),
                nullToEmpty(couponRate),
                nullToEmpty(maturityDate),
                nullToEmpty(totalSize),
                nullToEmpty(bookOpenDate),
                nullToEmpty(bookCloseDate));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // ----- Fluent setters --------------------------------------------------------

    public BondRecord isin(String v) { this.isin = v; return this; }
    public BondRecord issuerName(String v) { this.issuerName = v; return this; }
    public BondRecord bondName(String v) { this.bondName = v; return this; }
    public BondRecord currency(String v) { this.currency = v; return this; }
    public BondRecord faceValue(String v) { this.faceValue = v; return this; }
    public BondRecord faceValue(BigDecimal v) { this.faceValue = v.toPlainString(); return this; }
    public BondRecord couponRate(String v) { this.couponRate = v; return this; }
    public BondRecord couponRate(BigDecimal v) { this.couponRate = v.toPlainString(); return this; }
    public BondRecord maturityDate(String v) { this.maturityDate = v; return this; }
    public BondRecord maturityDate(LocalDate v) { this.maturityDate = v.toString(); return this; }
    public BondRecord totalSize(String v) { this.totalSize = v; return this; }
    public BondRecord totalSize(long v) { this.totalSize = Long.toString(v); return this; }
    public BondRecord bookOpenDate(String v) { this.bookOpenDate = v; return this; }
    public BondRecord bookOpenDate(LocalDate v) { this.bookOpenDate = v.toString(); return this; }
    public BondRecord bookCloseDate(String v) { this.bookCloseDate = v; return this; }
    public BondRecord bookCloseDate(LocalDate v) { this.bookCloseDate = v.toString(); return this; }

    // ----- Typed accessors (positive-path convenience) ---------------------------

    public BigDecimal faceValueDecimal() {
        return new BigDecimal(faceValue);
    }

    public BigDecimal couponRateDecimal() {
        return new BigDecimal(couponRate);
    }

    public long totalSizeLong() {
        return Long.parseLong(totalSize);
    }

    public LocalDate bookOpen() {
        return LocalDate.parse(bookOpenDate);
    }

    public LocalDate bookClose() {
        return LocalDate.parse(bookCloseDate);
    }

    public LocalDate maturity() {
        return LocalDate.parse(maturityDate);
    }

    @Override
    public String toString() {
        return "BondRecord{" + toCsvRow() + '}';
    }
}
