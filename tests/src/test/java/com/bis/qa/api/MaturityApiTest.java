package com.bis.qa.api;

import com.bis.qa.model.BondRecord;
import com.bis.qa.sftp.BondCsvBuilder;
import com.bis.qa.support.BondTestSupport;
import com.bis.qa.util.DateHelper;
import com.bis.qa.util.FinancialCalculator;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Maturity / principal return - PRODUCT.md section 9 and worked example 9A.
 */
@Epic("Maturity")
@Feature("Principal return")
public class MaturityApiTest extends BondTestSupport {

    @Test(groups = {"api", "allocation"})
    @Severity(SeverityLevel.BLOCKER)
    public void maturity_returnsPrincipalAndTransitionsToMatured() {
        LocalDate d = system.currentBusinessDate();
        // Choose a maturity date that is a business day.
        LocalDate maturity = DateHelper.nextBusinessDayOnOrAfter(d.plusDays(20));
        BondRecord bond = BondCsvBuilder
                .validBond(d.plusDays(1), d.plusDays(5), maturity)
                .totalSize(100_000).faceValue("1000.00").couponRate("0.0005");
        ingestBond(bond);
        system.advanceTo(d.plusDays(1));
        v1.subscribe(bond.isin, "INV-001", 100);
        closeAndAllocate(bond);

        system.advanceTo(bond.maturity());

        BigDecimal expected = FinancialCalculator.maturityPrincipal(new BigDecimal("1000.00"), 100);
        assertEquals(expected.stripTrailingZeros(), new BigDecimal("100000").stripTrailingZeros(),
                "oracle sanity: 1000 * 100 = 100,000");

        BigDecimal principal = maturityAmount("INV-001", bond.isin);
        assertNotNull(principal, "maturity payment should be readable from the portfolio");
        assertEquals(principal.stripTrailingZeros(), expected.stripTrailingZeros(),
                "Maturity principal should equal faceValue * allocatedQuantity");

        assertEquals(statusOf(bond.isin), "MATURED",
                "Bond should transition to MATURED after maturity processing");
    }

    @Test(groups = {"api", "allocation"})
    @Severity(SeverityLevel.CRITICAL)
    public void maturityOnWeekend_isPaidNextBusinessDay() {
        LocalDate d = system.currentBusinessDate();
        // Force a maturity date that lands on a Saturday.
        LocalDate saturday = nextSaturdayAfter(d.plusDays(20));
        BondRecord bond = BondCsvBuilder
                .validBond(d.plusDays(1), d.plusDays(5), saturday)
                .totalSize(100_000).faceValue("1000.00").couponRate("0.0005");
        ingestBond(bond);
        system.advanceTo(d.plusDays(1));
        v1.subscribe(bond.isin, "INV-002", 100);
        closeAndAllocate(bond);

        // Advance through the weekend to the next business day (Monday).
        LocalDate nextBiz = DateHelper.nextBusinessDayOnOrAfter(saturday);
        system.advanceTo(nextBiz);

        BigDecimal principal = maturityAmount("INV-002", bond.isin);
        assertNotNull(principal,
                "When maturity falls on a weekend, principal must be paid on the next business day "
                        + "(PRODUCT.md section 9)");
        assertEquals(principal.stripTrailingZeros(),
                new BigDecimal("100000").stripTrailingZeros(),
                "Maturity principal on the next business day should equal faceValue * quantity");
    }

    private static LocalDate nextSaturdayAfter(LocalDate from) {
        LocalDate d = from;
        while (d.getDayOfWeek() != java.time.DayOfWeek.SATURDAY) {
            d = d.plusDays(1);
        }
        return d;
    }
}
