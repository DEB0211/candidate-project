package com.bis.qa.api;

import com.bis.qa.model.BondRecord;
import com.bis.qa.support.BondTestSupport;
import com.bis.qa.util.FinancialCalculator;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Allocation rules - PRODUCT.md section 7 and worked example section 9A.
 */
@Epic("Allocation")
@Feature("Proportional allocation")
public class AllocationApiTest extends BondTestSupport {

    @Test(groups = {"api", "allocation"})
    @Severity(SeverityLevel.BLOCKER)
    public void oversubscribed_allocatesProportionallyPerWorkedExample() {
        // Reproduce PRODUCT.md section 9A: totalSize 100,000; subs 40k/30k/50k.
        BondRecord bond = ingestAndOpen(100_000);
        v1.subscribe(bond.isin, "INV-001", 40_000);
        v1.subscribe(bond.isin, "INV-002", 30_000);
        v1.subscribe(bond.isin, "INV-003", 50_000);

        closeAndAllocate(bond);

        Long a1 = allocatedQuantityFor("INV-001", bond.isin);
        Long a2 = allocatedQuantityFor("INV-002", bond.isin);
        Long a3 = allocatedQuantityFor("INV-003", bond.isin);

        assertNotNull(a1, "allocation for INV-001 should be readable from portfolio");
        assertNotNull(a2, "allocation for INV-002 should be readable from portfolio");
        assertNotNull(a3, "allocation for INV-003 should be readable from portfolio");

        assertEquals(a1.longValue(), 33_333L, "INV-001 expected floor(40000/120000*100000)");
        assertEquals(a2.longValue(), 25_000L, "INV-002 expected floor(30000/120000*100000)");
        assertEquals(a3.longValue(), 41_666L, "INV-003 expected floor(50000/120000*100000)");

        assertEquals(a1 + a2 + a3, 99_999L,
                "Total allocated should be 99,999 (rounding remainder unallocated)");
    }

    @Test(groups = {"api", "allocation"})
    @Severity(SeverityLevel.CRITICAL)
    public void undersubscribed_allocatesFullRequestedQuantity() {
        BondRecord bond = ingestAndOpen(100_000);
        v1.subscribe(bond.isin, "INV-001", 10_000);
        v1.subscribe(bond.isin, "INV-002", 20_000);

        closeAndAllocate(bond);

        assertEquals(allocatedQuantityFor("INV-001", bond.isin).longValue(), 10_000L,
                "Undersubscribed: investor should receive full requested quantity");
        assertEquals(allocatedQuantityFor("INV-002", bond.isin).longValue(), 20_000L,
                "Undersubscribed: investor should receive full requested quantity");
    }

    @Test(groups = {"api", "allocation"})
    @Severity(SeverityLevel.CRITICAL)
    public void zeroAllocation_isMarkedRejected() {
        // totalSize=2, subs 3 and 1 => floor(3/4*2)=1, floor(1/4*2)=0 => INV-002 REJECTED.
        BondRecord bond = ingestAndOpen(2);
        v1.subscribe(bond.isin, "INV-001", 3);
        v1.subscribe(bond.isin, "INV-002", 1);

        closeAndAllocate(bond);

        assertEquals(FinancialCalculator.proportionalAllocation(1, 4, 2), 0L,
                "oracle sanity: INV-002 floors to zero allocation");

        String status2 = subscriptionStatusFor("INV-002", bond.isin);
        assertEquals(status2, "REJECTED",
                "A subscriber allocated zero must be marked REJECTED (PRODUCT.md section 7)");
    }
}
