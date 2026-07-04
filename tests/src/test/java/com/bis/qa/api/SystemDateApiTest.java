package com.bis.qa.api;

import com.bis.qa.support.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.time.LocalDate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * System Control API - PRODUCT.md section 12.
 */
@Epic("System Control")
@Feature("Business date")
public class SystemDateApiTest extends BaseApiTest {

    @Test(groups = {"api", "smoke"})
    @Severity(SeverityLevel.BLOCKER)
    public void getDate_returnsValidBusinessDate() {
        Response r = system.getDate();
        assertEquals(r.statusCode(), 200, "GET /api/system/date should return 200");
        LocalDate date = system.currentBusinessDate();
        assertTrue(date != null, "business date must be present and parseable");
    }

    @Test(groups = {"api"})
    @Severity(SeverityLevel.CRITICAL)
    public void advanceDate_advancesByExactlyOneDay() {
        LocalDate before = system.currentBusinessDate();
        Response r = system.advanceDate();
        assertEquals(r.statusCode(), 200, "advance-date should return 200");
        LocalDate after = system.currentBusinessDate();
        assertEquals(after, before.plusDays(1),
                "advance-date must advance the business date by exactly one calendar day");
    }

    @Test(groups = {"api"})
    @Severity(SeverityLevel.NORMAL)
    public void reset_setsDateToTodayCalendarDate() {
        // Move the date forward a couple of days, then reset.
        system.advanceDate();
        system.advanceDate();
        Response r = system.reset();
        assertEquals(r.statusCode(), 200, "reset should return 200");
        LocalDate afterReset = system.currentBusinessDate();
        LocalDate today = LocalDate.now(com.bis.qa.config.TestConfig.businessZone());
        assertEquals(afterReset, today,
                "reset must set the business date back to today's calendar date (KL timezone)");
    }
}
