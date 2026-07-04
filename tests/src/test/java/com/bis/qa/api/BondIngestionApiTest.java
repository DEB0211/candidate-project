package com.bis.qa.api;

import com.bis.qa.model.BondRecord;
import com.bis.qa.sftp.BondCsvBuilder;
import com.bis.qa.support.BondTestSupport;
import com.bis.qa.util.IsinGenerator;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.time.LocalDate;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

/**
 * Bond creation via SFTP - PRODUCT.md section 5 (happy path + file-level rules).
 */
@Epic("Bond Ingestion")
@Feature("SFTP upload")
public class BondIngestionApiTest extends BondTestSupport {

    private BondRecord futureBond() {
        LocalDate today = system.currentBusinessDate();
        return BondCsvBuilder.validBond(
                today.plusDays(5), today.plusDays(15), today.plusDays(60));
    }

    @Test(groups = {"sftp", "smoke"})
    @Severity(SeverityLevel.BLOCKER)
    public void validBondFile_createsBondVisibleViaApi() {
        BondRecord bond = futureBond();
        ingestBond(bond);
        assertTrue(bondExists(bond.isin),
                "A valid uploaded bond should be created and visible via the API");
    }

    @Test(groups = {"sftp"})
    @Severity(SeverityLevel.CRITICAL)
    public void duplicateFileName_isNotReprocessed() {
        String sharedFileName = BondCsvBuilder.fileName();

        BondRecord first = futureBond();
        uploadCsv(sharedFileName, BondCsvBuilder.create().addRow(first).build());
        assertTrue(waitForBondVisible(first.isin), "first upload should ingest");

        // Same file name, different content: must be rejected (not reprocessed).
        BondRecord second = futureBond();
        uploadCsv(sharedFileName, BondCsvBuilder.create().addRow(second).build());
        sleep(TestConfigPoll());

        assertFalse(bondExists(second.isin),
                "A file with a previously used name must not be reprocessed (PRODUCT.md section 5)");
    }

    @Test(groups = {"sftp"})
    @Severity(SeverityLevel.CRITICAL)
    public void duplicateIsin_secondBondIsRejected() {
        // Spec section 5: ISIN must be unique across all bonds. Upload the same
        // ISIN twice (different files) and check the second one does not create a
        // second bond or overwrite the first.
        LocalDate today = system.currentBusinessDate();
        String isin = IsinGenerator.unique();

        BondRecord first = BondCsvBuilder.validBond(
                today.plusDays(5), today.plusDays(15), today.plusDays(60)).isin(isin);
        ingestBond(first);
        assertTrue(bondExists(isin), "first bond with this ISIN should be created");

        BondRecord clash = BondCsvBuilder.validBond(
                today.plusDays(6), today.plusDays(16), today.plusDays(61))
                .isin(isin).issuerName("Clashing Issuer");
        uploadCsv(BondCsvBuilder.create().addRow(clash).build());
        sleep(5000);

        // The original issuer must still be the one on record - the duplicate
        // must not have been ingested on top of it.
        String issuer = com.bis.qa.util.JsonFields.firstString(
                v1.getBond(isin), "issuer_name", "issuerName", "issuer");
        assertNotEquals(issuer, "Clashing Issuer",
                "A duplicate ISIN must not be reprocessed or overwrite the existing bond");
    }

    @Test(groups = {"sftp"})
    @Severity(SeverityLevel.NORMAL)
    public void boundaryValues_atMaxLimits_areAccepted() {
        // The negative suite covers just-over-the-limit rejections; this covers the
        // accept side of the same boundaries so we know validation is not simply
        // rejecting everything near the edge.
        LocalDate today = system.currentBusinessDate();
        BondRecord bond = BondCsvBuilder
                .validBond(today.plusDays(5), today.plusDays(15), today.plusDays(60))
                .faceValue("1000000.00")   // exactly the max
                .couponRate("0.9999")       // just under 1, 4 dp
                .totalSize(100_000_000L);   // exactly the max
        ingestBond(bond);
        assertTrue(bondExists(bond.isin),
                "A bond sitting exactly on the max faceValue/totalSize limits should be accepted");
    }

    @Test(groups = {"sftp"})
    @Severity(SeverityLevel.NORMAL)
    public void multipleBondsInOneFile_allCreated() {
        LocalDate today = system.currentBusinessDate();
        BondRecord a = BondCsvBuilder.validBond(today.plusDays(3), today.plusDays(10), today.plusDays(40))
                .isin(IsinGenerator.unique());
        BondRecord b = BondCsvBuilder.validBond(today.plusDays(3), today.plusDays(10), today.plusDays(40))
                .isin(IsinGenerator.unique());
        String csv = BondCsvBuilder.create().addRow(a).addRow(b).build();
        uploadCsv(csv);
        assertTrue(waitForBondVisible(a.isin) && bondExists(b.isin),
                "All valid rows in a single file should be ingested");
    }

    private long TestConfigPoll() {
        return Math.min(5, com.bis.qa.config.TestConfig.ingestionPollSeconds()) * 1000L;
    }
}
