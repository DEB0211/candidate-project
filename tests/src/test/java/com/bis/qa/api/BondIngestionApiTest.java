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
