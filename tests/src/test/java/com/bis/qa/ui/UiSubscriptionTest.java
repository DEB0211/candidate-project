package com.bis.qa.ui;

import com.bis.qa.model.BondRecord;
import com.bis.qa.support.BaseUiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * Web UI subscription flow (frontend on port 5173) - PRODUCT.md sections 6, 10, 11.
 *
 * <p>These tests exercise the investor journey through the browser and then
 * cross-check the resulting state via the API, so a UI-only regression cannot
 * hide a broken back end and vice versa.
 */
@Epic("Web UI")
@Feature("Subscription flow")
public class UiSubscriptionTest extends BaseUiTest {

    @Test(groups = {"ui", "smoke"})
    @Severity(SeverityLevel.CRITICAL)
    public void appLoadsAndListsBonds() {
        // Ensure at least one bond exists so the list is populated.
        ingestAndOpen(100_000);
        app.open(com.bis.qa.config.TestConfig.uiBaseUrl());
        assertTrue(app.bondCount() > 0, "The bond list should render at least one bond");
    }

    @Test(groups = {"ui"})
    @Severity(SeverityLevel.CRITICAL)
    public void investorCanSubscribeThroughUi() {
        BondRecord bond = ingestAndOpen(500_000);

        app.open(com.bis.qa.config.TestConfig.uiBaseUrl())
                .switchUser("INV-001")
                .openBond(bond.isin)
                .subscribe(1000);

        // The subscription must be reflected in the API portfolio for INV-001.
        String portfolio = v1.getPortfolio("INV-001").asString();
        assertTrue(portfolio.contains(bond.isin),
                "A subscription made via the UI should appear in the investor's portfolio via the API");
    }
}
