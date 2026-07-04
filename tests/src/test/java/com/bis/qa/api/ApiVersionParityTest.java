package com.bis.qa.api;

import com.bis.qa.model.BondRecord;
import com.bis.qa.support.BondTestSupport;
import com.bis.qa.util.JsonFields;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * API v1 vs v2 consistency - PRODUCT.md section 11.
 *
 * <p>v1 is snake_case/flat, v2 is camelCase/nested with string amounts. The two
 * versions must describe the same underlying bond identically in value.
 */
@Epic("API Contract")
@Feature("v1 / v2 parity")
public class ApiVersionParityTest extends BondTestSupport {

    @Test(groups = {"api", "parity"})
    @Severity(SeverityLevel.CRITICAL)
    public void bondDetail_isConsistentAcrossVersions() {
        BondRecord bond = ingestAndOpen(500_000);

        Response r1 = v1.getBond(bond.isin);
        Response r2 = v2.getBond(bond.isin);

        assertEquals(r1.statusCode(), 200, "v1 bond detail should be 200");
        assertEquals(r2.statusCode(), 200, "v2 bond detail should be 200");

        String isin1 = JsonFields.firstString(r1, "isin", "bond.isin");
        String isin2 = JsonFields.firstString(r2, "isin", "bond.isin");
        assertEquals(isin1, isin2, "ISIN must match across versions");
        assertEquals(isin1, bond.isin, "ISIN must match the uploaded bond");

        assertNumericEqual(
                JsonFields.firstString(r1, "face_value", "faceValue"),
                JsonFields.firstString(r2, "faceValue", "face_value", "bond.faceValue"),
                "faceValue must be numerically equal across versions");

        assertNumericEqual(
                JsonFields.firstString(r1, "coupon_rate", "couponRate"),
                JsonFields.firstString(r2, "couponRate", "coupon_rate", "bond.couponRate"),
                "couponRate must be numerically equal across versions");

        assertNumericEqual(
                JsonFields.firstString(r1, "total_size", "totalSize"),
                JsonFields.firstString(r2, "totalSize", "total_size", "bond.totalSize"),
                "totalSize must be numerically equal across versions");
    }

    @Test(groups = {"api", "parity"})
    @Severity(SeverityLevel.NORMAL)
    public void bondList_containsSameBondInBothVersions() {
        BondRecord bond = ingestAndOpen(100_000);

        Response l1 = v1.listBonds();
        Response l2 = v2.listBonds();

        assertEquals(l1.statusCode(), 200, "v1 list should be 200");
        assertEquals(l2.statusCode(), 200, "v2 list should be 200");

        org.testng.Assert.assertTrue(l1.asString().contains(bond.isin),
                "v1 bond list should contain the uploaded ISIN");
        org.testng.Assert.assertTrue(l2.asString().contains(bond.isin),
                "v2 bond list should contain the uploaded ISIN");
    }

    private void assertNumericEqual(String v1Value, String v2Value, String message) {
        assertNotNull(v1Value, message + " (v1 value missing)");
        assertNotNull(v2Value, message + " (v2 value missing)");
        assertEquals(new BigDecimal(v1Value).compareTo(new BigDecimal(v2Value)), 0, message);
    }
}
