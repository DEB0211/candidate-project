package com.bis.qa.support;

import com.bis.qa.clients.BondsApiV1Client;
import com.bis.qa.clients.SystemApiClient;
import com.bis.qa.model.BondRecord;
import com.bis.qa.sftp.BondCsvBuilder;
import com.bis.qa.sftp.SftpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Single source of truth for creating and driving bonds in tests, shared by the
 * API-layer base class ({@link BondTestSupport}) and the UI base class
 * ({@link BaseUiTest}). Keeps the SFTP-upload / poll / advance-date logic in one
 * place to avoid duplication.
 */
public final class BondFixture {

    private BondFixture() {
    }

    @Step("Upload single-bond CSV via SFTP")
    public static void upload(BondRecord bond) {
        String csv = BondCsvBuilder.create().addRow(bond).build();
        try (SftpClient sftp = new SftpClient()) {
            sftp.upload(BondCsvBuilder.fileName(), csv);
        }
    }

    public static boolean bondExists(BondsApiV1Client v1, String isin) {
        Response detail = v1.getBond(isin);
        if (detail.statusCode() == 200) {
            return true;
        }
        Response list = v1.listBonds();
        return list.statusCode() == 200 && list.asString().contains(isin);
    }

    @Step("Wait until bond {1} is visible via API")
    public static boolean waitForBondVisible(BondsApiV1Client v1, String isin, int pollSeconds) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(pollSeconds));
        while (Instant.now().isBefore(deadline)) {
            if (bondExists(v1, isin)) {
                return true;
            }
            sleep(1000);
        }
        return bondExists(v1, isin);
    }

    /** Uploads a bond and blocks until it is visible via the API. */
    public static BondRecord ingest(BondsApiV1Client v1, BondRecord bond, int pollSeconds) {
        upload(bond);
        waitForBondVisible(v1, bond.isin, pollSeconds);
        return bond;
    }

    /**
     * Ingests a bond whose book opens one day in the future (relative to the live
     * business date) and advances the date until the book is OPEN.
     */
    @Step("Ingest and open a bond (totalSize {2})")
    public static BondRecord ingestAndOpen(SystemApiClient system, BondsApiV1Client v1,
                                           long totalSize, int pollSeconds) {
        LocalDate d = system.currentBusinessDate();
        BondRecord bond = BondCsvBuilder
                .validBond(d.plusDays(1), d.plusDays(8), d.plusDays(40))
                .totalSize(totalSize);
        ingest(v1, bond, pollSeconds);
        system.advanceTo(d.plusDays(1));
        return bond;
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
