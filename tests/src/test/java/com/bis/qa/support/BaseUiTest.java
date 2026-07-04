package com.bis.qa.support;

import com.bis.qa.clients.BondsApiV1Client;
import com.bis.qa.clients.SystemApiClient;
import com.bis.qa.config.TestConfig;
import com.bis.qa.pages.AppPage;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.ByteArrayInputStream;
import java.time.Duration;

/**
 * Base class for Selenium UI tests.
 *
 * <p>Selenium 4's built-in Selenium Manager resolves the driver binary
 * automatically, so no WebDriverManager dependency is required. A fresh driver
 * is created per test method for isolation, and a screenshot is attached to
 * Allure on failure.
 */
public abstract class BaseUiTest {

    protected WebDriver driver;
    protected AppPage app;

    protected final SystemApiClient system = new SystemApiClient();
    protected final BondsApiV1Client v1 = new BondsApiV1Client();

    /** Creates an OPEN bond via SFTP + business-date advance (shared fixture). */
    protected com.bis.qa.model.BondRecord ingestAndOpen(long totalSize) {
        return BondFixture.ingestAndOpen(system, v1, totalSize,
                TestConfig.ingestionPollSeconds());
    }

    @BeforeMethod(alwaysRun = true)
    public void setUpDriver() {
        driver = createDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(TestConfig.uiTimeoutSeconds()));
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(1400, 1000));
        app = new AppPage(driver);
        app.open(TestConfig.uiBaseUrl());
    }

    private WebDriver createDriver() {
        String browser = TestConfig.uiBrowser().toLowerCase();
        boolean headless = TestConfig.uiHeadless();
        if (browser.contains("firefox")) {
            FirefoxOptions options = new FirefoxOptions();
            if (headless) {
                options.addArguments("-headless");
            }
            return new org.openqa.selenium.firefox.FirefoxDriver(options);
        }
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu",
                "--window-size=1400,1000");
        return new org.openqa.selenium.chrome.ChromeDriver(options);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownDriver(ITestResult result) {
        if (driver != null) {
            if (!result.isSuccess() && driver instanceof TakesScreenshot ts) {
                try {
                    byte[] png = ts.getScreenshotAs(OutputType.BYTES);
                    Allure.addAttachment("Failure screenshot", "image/png",
                            new ByteArrayInputStream(png), "png");
                } catch (Exception ignored) {
                    // best effort
                }
            }
            driver.quit();
        }
    }
}
