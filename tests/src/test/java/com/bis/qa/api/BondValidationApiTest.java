package com.bis.qa.api;

import com.bis.qa.model.BondRecord;
import com.bis.qa.sftp.BondCsvBuilder;
import com.bis.qa.support.BondTestSupport;
import com.bis.qa.util.IsinGenerator;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDate;

import static org.testng.Assert.assertFalse;

/**
 * File-level validation of bond ingestion - PRODUCT.md section 5.
 *
 * <p>Each negative case uploads a CSV that violates exactly one rule and asserts
 * the offending bond is NOT created. The spec states malformed rows must produce
 * file-level exceptions and not be silently skipped, so no invalid bond should
 * ever appear via the API.
 */
@Epic("Bond Ingestion")
@Feature("CSV validation")
public class BondValidationApiTest extends BondTestSupport {

    private static BondRecord baseValid(String isin) {
        LocalDate today = LocalDate.now();
        return new BondRecord()
                .isin(isin)
                .issuerName("Validation Issuer")
                .bondName("Validation Bond")
                .currency("MYR")
                .faceValue("1000.00")
                .couponRate("0.0005")
                .maturityDate(today.plusDays(60))
                .totalSize(100_000)
                .bookOpenDate(today.plusDays(5))
                .bookCloseDate(today.plusDays(15));
    }

    @DataProvider(name = "invalidBonds")
    public Object[][] invalidBonds() {
        LocalDate today = LocalDate.now();

        String shortIsin = IsinGenerator.ofLength(11);
        String longIsin = IsinGenerator.ofLength(13);
        String badCcy = IsinGenerator.unique();
        String negFace = IsinGenerator.unique();
        String bigFace = IsinGenerator.unique();
        String decFace = IsinGenerator.unique();
        String nanFace = IsinGenerator.unique();
        String zeroRate = IsinGenerator.unique();
        String oneRate = IsinGenerator.unique();
        String bigRate = IsinGenerator.unique();
        String precRate = IsinGenerator.unique();
        String zeroSize = IsinGenerator.unique();
        String negSize = IsinGenerator.unique();
        String hugeSize = IsinGenerator.unique();
        String decSize = IsinGenerator.unique();
        String openAfterClose = IsinGenerator.unique();
        String matBeforeClose = IsinGenerator.unique();
        String badDate = IsinGenerator.unique();

        return new Object[][]{
                {"ISIN shorter than 12 chars", csv(baseValid(shortIsin)), shortIsin},
                {"ISIN longer than 12 chars", csv(baseValid(longIsin)), longIsin},
                {"Empty ISIN", csv(baseValid("")), ",Validation Issuer"},
                {"Invalid 2-letter currency", csv(baseValid(badCcy).currency("MY")), badCcy},
                {"Negative faceValue", csv(baseValid(negFace).faceValue("-1000.00")), negFace},
                {"faceValue above 1,000,000 max", csv(baseValid(bigFace).faceValue("1000000.01")), bigFace},
                {"faceValue with 3 decimals", csv(baseValid(decFace).faceValue("1000.123")), decFace},
                {"Non-numeric faceValue", csv(baseValid(nanFace).faceValue("not_a_number")), nanFace},
                {"couponRate = 0 (not exclusive)", csv(baseValid(zeroRate).couponRate("0")), zeroRate},
                {"couponRate = 1 (not exclusive)", csv(baseValid(oneRate).couponRate("1")), oneRate},
                {"couponRate > 1", csv(baseValid(bigRate).couponRate("1.5")), bigRate},
                {"couponRate with 5 decimals", csv(baseValid(precRate).couponRate("0.00005")), precRate},
                {"totalSize = 0", csv(baseValid(zeroSize).totalSize("0")), zeroSize},
                {"Negative totalSize", csv(baseValid(negSize).totalSize("-100")), negSize},
                {"totalSize above 100,000,000 max", csv(baseValid(hugeSize).totalSize("100000001")), hugeSize},
                {"Non-integer totalSize", csv(baseValid(decSize).totalSize("100.5")), decSize},
                {"bookOpenDate after bookCloseDate",
                        csv(baseValid(openAfterClose)
                                .bookOpenDate(today.plusDays(20))
                                .bookCloseDate(today.plusDays(10))), openAfterClose},
                {"maturityDate before bookCloseDate",
                        csv(baseValid(matBeforeClose)
                                .maturityDate(today.plusDays(5))
                                .bookCloseDate(today.plusDays(15))), matBeforeClose},
                {"Invalid date format", csv(baseValid(badDate).maturityDate("05-06-2026")), badDate},
                {"Missing header row",
                        BondCsvBuilder.create().withHeader(false)
                                .addRow(baseValid(IsinGenerator.unique())).build(),
                        "Validation Issuer"},
                {"Malformed row - too few columns",
                        BondCsvBuilder.create()
                                .addRawRow("MYBND0000BAD,MALFORMED,Missing Fields,MYR,1000.00,0.0005").build(),
                        "MYBND0000BAD"},
        };
    }

    private static String csv(BondRecord record) {
        return BondCsvBuilder.create().addRow(record).build();
    }

    @Test(dataProvider = "invalidBonds", groups = {"sftp"})
    @Severity(SeverityLevel.CRITICAL)
    public void invalidBond_isRejected(String description, String csvContent, String probe) {
        uploadCsv(csvContent);
        // Give the batch a brief window; invalid content must never surface.
        sleep(5000);
        assertFalse(bondExists(probe),
                "Invalid bond should be rejected by ingestion validation: " + description);
    }
}
