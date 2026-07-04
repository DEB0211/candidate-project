package com.bis.qa.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Independent financial oracle used to verify the system's money math
 * (PRODUCT.md sections 7, 8, 9 and 13).
 *
 * <p>All arithmetic uses {@link BigDecimal} exclusively - no floating point -
 * so the expected values computed here are exact and can be compared against
 * the amounts the system returns.
 */
public final class FinancialCalculator {

    private FinancialCalculator() {
    }

    /**
     * Proportional allocation per PRODUCT.md section 7:
     * {@code floor(theirQuantity / totalSubscribed * totalSize)}.
     *
     * <p>Computed with high precision then floored, matching the worked example
     * in section 9A (e.g. 40000/120000*100000 -> 33333).
     */
    public static long proportionalAllocation(long theirQuantity, long totalSubscribed, long totalSize) {
        if (totalSubscribed <= 0) {
            return 0L;
        }
        if (totalSubscribed <= totalSize) {
            // Not oversubscribed: full allocation.
            return theirQuantity;
        }
        BigDecimal q = BigDecimal.valueOf(theirQuantity);
        BigDecimal total = BigDecimal.valueOf(totalSubscribed);
        BigDecimal size = BigDecimal.valueOf(totalSize);
        return q.divide(total, 20, RoundingMode.HALF_UP)
                .multiply(size)
                .setScale(0, RoundingMode.FLOOR)
                .longValueExact();
    }

    /**
     * Daily coupon per subscriber (section 8):
     * {@code faceValue * couponRate * allocatedQuantity}.
     */
    public static BigDecimal dailyCoupon(BigDecimal faceValue, BigDecimal couponRate, long allocatedQuantity) {
        return faceValue
                .multiply(couponRate)
                .multiply(BigDecimal.valueOf(allocatedQuantity));
    }

    /**
     * Total coupon paid across the inclusive range of business days
     * [firstCouponDate, maturityDate]. Coupon is paid up to and including the
     * maturity date when it is a business day (section 8).
     */
    public static BigDecimal totalCoupon(BigDecimal faceValue,
                                         BigDecimal couponRate,
                                         long allocatedQuantity,
                                         LocalDate firstCouponDate,
                                         LocalDate maturityDate) {
        int businessDays = DateHelper.businessDaysBetweenInclusive(firstCouponDate, maturityDate);
        return dailyCoupon(faceValue, couponRate, allocatedQuantity)
                .multiply(BigDecimal.valueOf(businessDays));
    }

    /**
     * Principal returned at maturity (section 9):
     * {@code faceValue * allocatedQuantity}.
     */
    public static BigDecimal maturityPrincipal(BigDecimal faceValue, long allocatedQuantity) {
        return faceValue.multiply(BigDecimal.valueOf(allocatedQuantity));
    }
}
