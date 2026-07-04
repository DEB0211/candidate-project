package com.bis.qa.api;

import com.bis.qa.model.BondRecord;
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
import static org.testng.Assert.assertTrue;

/**
 * Daily coupon payments - PRODUCT.md section 8 and worked example 9A.
 */
@Epic("Coupon Payments")
@Feature("Daily coupon accrual")
public class CouponPaymentApiTest extends BondTestSupport {

    @Test(groups = {"api", "allocation"})
    @Severity(SeverityLevel.BLOCKER)
    public void dailyCoupon_matchesExactDecimalFormula() {
        // faceValue 1000.00, couponRate 0.0005, single subscriber gets full 100.
        LocalDate d = system.currentBusinessDate();
        BondRecord bond = com.bis.qa.sftp.BondCsvBuilder
                .validBond(d.plusDays(1), d.plusDays(3), d.plusDays(30))
                .totalSize(100_000)
                .faceValue("1000.00")
                .couponRate("0.0005");
        ingestBond(bond);
        system.advanceTo(d.plusDays(1));

        v1.subscribe(bond.isin, "INV-001", 100);
        closeAndAllocate(bond);

        // Advance exactly one business day so exactly one coupon accrues.
        LocalDate afterAlloc = system.currentBusinessDate();
        LocalDate oneBizDay = DateHelper.nextBusinessDay(afterAlloc);
        system.advanceTo(oneBizDay);

        BigDecimal expectedDaily = FinancialCalculator.dailyCoupon(
                new BigDecimal("1000.00"), new BigDecimal("0.0005"), 100); // 50.00
        assertEquals(expectedDaily.stripTrailingZeros(), new BigDecimal("50").stripTrailingZeros(),
                "oracle sanity: 1000 * 0.0005 * 100 = 50.00");

        BigDecimal received = totalCouponReceived("INV-001", bond.isin);
        assertNotNull(received, "coupon payments should be readable from the portfolio");
        assertEquals(received.stripTrailingZeros(), expectedDaily.stripTrailingZeros(),
                "One business day of coupon should equal faceValue*couponRate*qty (50.00)");
    }

    @Test(groups = {"api", "allocation"})
    @Severity(SeverityLevel.CRITICAL)
    public void coupon_isNotPaidOnWeekends() {
        LocalDate d = system.currentBusinessDate();
        BondRecord bond = com.bis.qa.sftp.BondCsvBuilder
                .validBond(d.plusDays(1), d.plusDays(3), d.plusDays(40))
                .totalSize(100_000).faceValue("1000.00").couponRate("0.0005");
        ingestBond(bond);
        system.advanceTo(d.plusDays(1));
        v1.subscribe(bond.isin, "INV-002", 100);
        closeAndAllocate(bond);

        LocalDate start = system.currentBusinessDate();
        // Advance a full 7 calendar days, spanning at least one weekend.
        LocalDate end = start.plusDays(7);
        system.advanceTo(end);

        int businessDays = DateHelper.businessDaysBetweenInclusive(
                DateHelper.nextBusinessDayOnOrAfter(start.plusDays(1)), end);
        BigDecimal expected = FinancialCalculator.dailyCoupon(
                        new BigDecimal("1000.00"), new BigDecimal("0.0005"), 100)
                .multiply(BigDecimal.valueOf(businessDays));

        BigDecimal received = totalCouponReceived("INV-002", bond.isin);
        assertNotNull(received, "coupon payments should be readable from the portfolio");
        assertEquals(received.stripTrailingZeros(), expected.stripTrailingZeros(),
                "Coupons must accrue only on business days (weekends skipped) over the span");
    }

    @Test(groups = {"api", "allocation"})
    @Severity(SeverityLevel.NORMAL)
    public void coupon_accruesEveryBusinessDayUntilMaturity() {
        LocalDate d = system.currentBusinessDate();
        BondRecord bond = com.bis.qa.sftp.BondCsvBuilder
                .validBond(d.plusDays(1), d.plusDays(3), d.plusDays(20))
                .totalSize(100_000).faceValue("1000.00").couponRate("0.0005");
        ingestBond(bond);
        system.advanceTo(d.plusDays(1));
        v1.subscribe(bond.isin, "INV-003", 100);
        closeAndAllocate(bond);

        LocalDate firstCoupon = DateHelper.nextBusinessDayOnOrAfter(
                system.currentBusinessDate().plusDays(1));
        system.advanceTo(bond.maturity());

        BigDecimal expected = FinancialCalculator.totalCoupon(
                new BigDecimal("1000.00"), new BigDecimal("0.0005"), 100,
                firstCoupon, bond.maturity());

        BigDecimal received = totalCouponReceived("INV-003", bond.isin);
        assertNotNull(received, "coupon payments should be readable from the portfolio");
        assertTrue(received.compareTo(BigDecimal.ZERO) > 0, "some coupon should have accrued");
        assertEquals(received.stripTrailingZeros(), expected.stripTrailingZeros(),
                "Total coupon should equal daily amount * business days through maturity");
    }
}
