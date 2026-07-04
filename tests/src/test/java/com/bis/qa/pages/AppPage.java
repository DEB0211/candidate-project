package com.bis.qa.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page object for the Bond Issuance web app (frontend on port 5173).
 *
 * <p>The app is a single-page investor UI with a user switcher in the header, a
 * bond list, a subscribe form, and a portfolio view (PRODUCT.md sections 10-11).
 *
 * <p><b>Locator strategy:</b> the exact DOM is not documented, so locators are
 * resilient - they prefer stable {@code data-testid} hooks and fall back to
 * accessible text. If the real markup differs, adjusting the {@code By}
 * constants here fixes every UI test without touching test logic.
 */
public class AppPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Preferred stable hooks (data-testid) with text-based fallbacks.
    private static final By USER_SWITCHER = By.cssSelector(
            "[data-testid='user-switcher'], select[name*='user' i], #user-switcher");
    private static final By BOND_ROWS = By.cssSelector(
            "[data-testid='bond-row'], table tbody tr, .bond-list .bond-item");
    private static final By QUANTITY_INPUT = By.cssSelector(
            "[data-testid='subscribe-quantity'], input[name*='quantity' i], input[type='number']");
    private static final By SUBSCRIBE_BUTTON = By.cssSelector(
            "[data-testid='subscribe-submit'], button[type='submit']");
    private static final By PORTFOLIO_LINK = By.cssSelector(
            "[data-testid='nav-portfolio'], a[href*='portfolio' i]");
    private static final By PORTFOLIO_ROWS = By.cssSelector(
            "[data-testid='portfolio-row'], .portfolio table tbody tr, .portfolio-item");
    private static final By TOAST = By.cssSelector(
            "[data-testid='toast'], .toast, .notification, [role='alert']");

    public AppPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @Step("Open web app at {0}")
    public AppPage open(String baseUrl) {
        driver.get(baseUrl);
        return this;
    }

    @Step("Switch to user {0}")
    public AppPage switchUser(String userId) {
        WebElement switcher = wait.until(
                ExpectedConditions.presenceOfElementLocated(USER_SWITCHER));
        if ("select".equalsIgnoreCase(switcher.getTagName())) {
            new org.openqa.selenium.support.ui.Select(switcher).selectByValue(userId);
        } else {
            switcher.click();
            driver.findElement(By.xpath(
                    "//*[contains(normalize-space(.),'" + userId + "')]")).click();
        }
        return this;
    }

    @Step("Count visible bond rows")
    public int bondCount() {
        return driver.findElements(BOND_ROWS).size();
    }

    public List<WebElement> bondRows() {
        return driver.findElements(BOND_ROWS);
    }

    @Step("Open subscribe form for bond {0}")
    public AppPage openBond(String isin) {
        WebElement row = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(normalize-space(.),'" + isin + "')]")));
        row.click();
        return this;
    }

    @Step("Submit subscription quantity {0}")
    public AppPage subscribe(long quantity) {
        WebElement qty = wait.until(ExpectedConditions.presenceOfElementLocated(QUANTITY_INPUT));
        qty.clear();
        qty.sendKeys(Long.toString(quantity));
        wait.until(ExpectedConditions.elementToBeClickable(SUBSCRIBE_BUTTON)).click();
        return this;
    }

    @Step("Read notification / toast message")
    public String notificationText() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(TOAST)).getText();
        } catch (Exception e) {
            return "";
        }
    }

    @Step("Navigate to portfolio")
    public AppPage openPortfolio() {
        wait.until(ExpectedConditions.elementToBeClickable(PORTFOLIO_LINK)).click();
        return this;
    }

    @Step("Count portfolio rows")
    public int portfolioRowCount() {
        return driver.findElements(PORTFOLIO_ROWS).size();
    }

    public String pageSource() {
        return driver.getPageSource();
    }
}
