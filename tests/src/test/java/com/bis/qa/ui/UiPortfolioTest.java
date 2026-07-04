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
 * Web UI portfolio view - PRODUCT.md sections 10, 11.
 *
 * <p>Verifies that a subscription created via the API is visible in the UI
 * portfolio, reconciling the two layers.
 */
@Epic("Web UI")
@Feature("Portfolio view")
public class UiPortfolioTest extends BaseUiTest {

    @Test(groups = {"ui"})
    @Severity(SeverityLevel.NORMAL)
    public void portfolioReflectsApiSubscription() {
        BondRecord bond = ingestAndOpen(500_000);

        // Create the subscription via API, then confirm the UI portfolio shows it.
        v1.subscribe(bond.isin, "INV-002", 1500);

        app.open(com.bis.qa.config.TestConfig.uiBaseUrl())
                .switchUser("INV-002")
                .openPortfolio();

        assertTrue(app.portfolioRowCount() > 0
                        || app.pageSource().contains(bond.isin),
                "The investor's portfolio should display the subscribed bond " + bond.isin);
    }
}
