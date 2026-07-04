package com.bis.qa.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Business-date arithmetic mirroring PRODUCT.md sections 8, 9 and 12.
 *
 * <p>Weekends (Saturday and Sunday) are not business days. Public holidays are
 * out of scope per the specification.
 */
public final class DateHelper {

    private DateHelper() {
    }

    public static boolean isBusinessDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    public static boolean isWeekend(LocalDate date) {
        return !isBusinessDay(date);
    }

    /** The given date if it is a business day, otherwise the next business day. */
    public static LocalDate nextBusinessDayOnOrAfter(LocalDate date) {
        LocalDate d = date;
        while (isWeekend(d)) {
            d = d.plusDays(1);
        }
        return d;
    }

    /** Strictly the next business day after the given date. */
    public static LocalDate nextBusinessDay(LocalDate date) {
        LocalDate d = date.plusDays(1);
        while (isWeekend(d)) {
            d = d.plusDays(1);
        }
        return d;
    }

    /**
     * Number of business days in the inclusive range [start, end].
     * Returns 0 when end is before start.
     */
    public static int businessDaysBetweenInclusive(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            return 0;
        }
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (isBusinessDay(d)) {
                count++;
            }
        }
        return count;
    }
}
