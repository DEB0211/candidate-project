package com.bis.qa.api;

import com.bis.qa.model.BondRecord;
import com.bis.qa.support.BondTestSupport;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

/**
 * Subscription rules - PRODUCT.md section 6.
 */
@Epic("Subscriptions")
@Feature("Subscription rules")
public class SubscriptionApiTest extends BondTestSupport {

    @Test(groups = {"api", "smoke"})
    @Severity(SeverityLevel.BLOCKER)
    public void subscribeWithinWindow_succeeds() {
        BondRecord bond = ingestAndOpen(100_000);
        Response r = v1.subscribe(bond.isin, "INV-001", 1000);
        assertTrue(r.statusCode() >= 200 && r.statusCode() < 300,
                "Subscription within the book window should succeed, got " + r.statusCode());
    }

    @Test(groups = {"api"})
    @Severity(SeverityLevel.CRITICAL)
    public void subscribeMissingUserHeader_isRejected() {
        BondRecord bond = ingestAndOpen(100_000);
        Response r = v1.subscribeRaw(bond.isin, null, Map.of("quantity", 100));
        assertNotEquals(r.statusCode() / 100, 2,
                "Subscription without X-User-Id must be rejected (PRODUCT.md section 6)");
    }

    @Test(groups = {"api"})
    @Severity(SeverityLevel.CRITICAL)
    public void subscribeNonPositiveQuantity_isRejected() {
        BondRecord bond = ingestAndOpen(100_000);
        Response zero = v1.subscribe(bond.isin, "INV-002", 0);
        Response neg = v1.subscribe(bond.isin, "INV-003", -5);
        assertNotEquals(zero.statusCode() / 100, 2, "quantity=0 must be rejected");
        assertNotEquals(neg.statusCode() / 100, 2, "negative quantity must be rejected");
    }

    @Test(groups = {"api"})
    @Severity(SeverityLevel.CRITICAL)
    public void duplicateSubscriptionSameInvestor_isRejected() {
        BondRecord bond = ingestAndOpen(100_000);
        Response first = v1.subscribe(bond.isin, "INV-004", 1000);
        assertEquals(first.statusCode() / 100, 2, "first subscription should succeed");
        Response second = v1.subscribe(bond.isin, "INV-004", 500);
        assertNotEquals(second.statusCode() / 100, 2,
                "an investor may only subscribe once per bond (PRODUCT.md section 6)");
    }

    @Test(groups = {"api"})
    @Severity(SeverityLevel.CRITICAL)
    public void subscribeBeforeBookOpens_isRejected() {
        // Bond that is still PENDING (book opens in the future).
        io.restassured.response.Response ignored;
        java.time.LocalDate d = system.currentBusinessDate();
        BondRecord pending = com.bis.qa.sftp.BondCsvBuilder
                .validBond(d.plusDays(5), d.plusDays(12), d.plusDays(40))
                .totalSize(100_000);
        ingestBond(pending);
        Response r = v1.subscribe(pending.isin, "INV-005", 100);
        assertNotEquals(r.statusCode() / 100, 2,
                "Subscriptions before the book opens must be rejected (PRODUCT.md section 6)");
    }

    @Test(groups = {"api"})
    @Severity(SeverityLevel.CRITICAL)
    public void subscribeAfterBookCloses_isRejected() {
        BondRecord bond = ingestAndOpen(100_000);
        // Move past the close date so the book is CLOSED.
        system.advanceTo(bond.bookClose().plusDays(1));
        Response r = v1.subscribe(bond.isin, "INV-001", 100);
        assertNotEquals(r.statusCode() / 100, 2,
                "No new subscriptions once the book is closed (PRODUCT.md section 6)");
    }
}
