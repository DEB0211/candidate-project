package com.bis.qa.support;

import com.bis.qa.model.BondRecord;
import com.bis.qa.sftp.BondCsvBuilder;
import com.bis.qa.sftp.SftpClient;
import com.bis.qa.config.TestConfig;
import com.bis.qa.util.JsonFields;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/**
 * Reusable lifecycle helpers shared by the API tests: upload a bond via SFTP,
 * wait for ingestion, and drive a bond through its states by advancing the
 * business date (PRODUCT.md sections 3 and 12).
 *
 * <p>Extends {@link BaseApiTest} so tests can gain both the clients and these
 * helpers from a single parent.
 */
public abstract class BondTestSupport extends BaseApiTest {

    /**
     * Uploads a single-bond CSV and blocks until the bond is visible through the
     * v1 API (or the ingestion poll window elapses). Delegates to
     * {@link BondFixture} so API and UI tests share one implementation.
     *
     * @return the uploaded {@link BondRecord} (with its generated ISIN).
     */
    protected BondRecord ingestBond(BondRecord bond) {
        return BondFixture.ingest(v1, bond, TestConfig.ingestionPollSeconds());
    }

    /** Uploads arbitrary CSV content under a generated file name; no wait. */
    @Step("Upload raw CSV file {0}")
    protected String uploadCsv(String csvContent) {
        String fileName = BondCsvBuilder.fileName();
        try (SftpClient sftp = new SftpClient()) {
            sftp.upload(fileName, csvContent);
        }
        return fileName;
    }

    /** Uploads CSV content under a caller-specified file name; no wait. */
    @Step("Upload CSV file {0}")
    protected void uploadCsv(String fileName, String csvContent) {
        try (SftpClient sftp = new SftpClient()) {
            sftp.upload(fileName, csvContent);
        }
    }

    /** Polls the v1 bond list until the ISIN appears or the timeout elapses. */
    protected boolean waitForBondVisible(String isin) {
        return BondFixture.waitForBondVisible(v1, isin, TestConfig.ingestionPollSeconds());
    }

    /**
     * Ingests a bond whose book opens one business day in the future (relative to
     * the current business date) and advances the date until the book is OPEN.
     *
     * <p>Book dates are always computed from the live business date, so this works
     * no matter how far previous sequential tests advanced the global date.
     */
    protected BondRecord ingestAndOpen(long totalSize) {
        return BondFixture.ingestAndOpen(system, v1, totalSize, TestConfig.ingestionPollSeconds());
    }

    /** Advances the date past the bond's book close so it is CLOSED then ALLOCATED. */
    @Step("Close book and allocate bond {0}")
    protected void closeAndAllocate(BondRecord bond) {
        system.advanceTo(bond.bookClose().plusDays(1));
    }

    /** Reads the bond's lifecycle status via the v1 API (status/state field). */
    protected String statusOf(String isin) {
        Response detail = v1.getBond(isin);
        return JsonFields.firstString(detail, "status", "state", "bond_status");
    }

    /**
     * Reads the allocated quantity for {@code userId} on bond {@code isin} from the
     * v1 portfolio. Tries several GPath shapes because the exact portfolio schema
     * is not documented.
     */
    protected Long allocatedQuantityFor(String userId, String isin) {
        Response portfolio = v1.getPortfolio(userId);
        return JsonFields.firstLong(portfolio,
                "subscriptions.find { it.isin == '" + isin + "' }.allocated_quantity",
                "subscriptions.find { it.isin == '" + isin + "' }.allocatedQuantity",
                "find { it.isin == '" + isin + "' }.allocated_quantity",
                "find { it.isin == '" + isin + "' }.allocatedQuantity",
                "subscriptions.find { it.bond_isin == '" + isin + "' }.allocated_quantity");
    }

    /** Reads the subscription status (ALLOCATED / REJECTED / PENDING) for a user + bond. */
    protected String subscriptionStatusFor(String userId, String isin) {
        Response portfolio = v1.getPortfolio(userId);
        return JsonFields.firstString(portfolio,
                "subscriptions.find { it.isin == '" + isin + "' }.status",
                "subscriptions.find { it.isin == '" + isin + "' }.state",
                "find { it.isin == '" + isin + "' }.status",
                "subscriptions.find { it.bond_isin == '" + isin + "' }.status");
    }

    /**
     * Sums all coupon payments received by {@code userId} for bond {@code isin},
     * read from the v1 portfolio. Amounts may be numeric or string (v2 style);
     * both are summed with {@link java.math.BigDecimal}.
     */
    protected java.math.BigDecimal totalCouponReceived(String userId, String isin) {
        Response portfolio = v1.getPortfolio(userId);
        return sumAmounts(portfolio,
                "coupons.findAll { it.isin == '" + isin + "' }.amount",
                "coupon_payments.findAll { it.isin == '" + isin + "' }.amount",
                "couponPayments.findAll { it.isin == '" + isin + "' }.amount",
                "coupons.findAll { it.bond_isin == '" + isin + "' }.amount");
    }

    /** Reads the maturity/principal amount returned to {@code userId} for a bond. */
    protected java.math.BigDecimal maturityAmount(String userId, String isin) {
        Response portfolio = v1.getPortfolio(userId);
        return sumAmounts(portfolio,
                "maturities.findAll { it.isin == '" + isin + "' }.amount",
                "maturity_payments.findAll { it.isin == '" + isin + "' }.amount",
                "maturities.findAll { it.isin == '" + isin + "' }.principal",
                "maturityPayments.findAll { it.isin == '" + isin + "' }.amount");
    }

    private java.math.BigDecimal sumAmounts(Response response, String... candidateGpaths) {
        io.restassured.path.json.JsonPath jp = response.jsonPath();
        for (String path : candidateGpaths) {
            try {
                java.util.List<Object> values = jp.getList(path);
                if (values != null && !values.isEmpty()) {
                    java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
                    for (Object v : values) {
                        if (v != null) {
                            sum = sum.add(new java.math.BigDecimal(v.toString()));
                        }
                    }
                    return sum;
                }
            } catch (Exception ignored) {
                // try next candidate path
            }
        }
        return null;
    }

    protected boolean bondExists(String isin) {
        return BondFixture.bondExists(v1, isin);
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
