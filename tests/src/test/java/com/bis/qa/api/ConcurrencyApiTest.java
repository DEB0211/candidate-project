package com.bis.qa.api;

import com.bis.qa.model.BondRecord;
import com.bis.qa.support.BondTestSupport;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertTrue;

/**
 * Concurrency / atomicity of available size - PRODUCT.md section 6:
 * "concurrent subscriptions exceeding remaining capacity must not all succeed".
 */
@Epic("Subscriptions")
@Feature("Concurrency / atomicity")
public class ConcurrencyApiTest extends BondTestSupport {

    @Test(groups = {"api", "concurrency"})
    @Severity(SeverityLevel.CRITICAL)
    public void concurrentSubscriptions_doNotOversell() throws Exception {
        long totalSize = 100;
        long perOrder = 30; // 5 investors * 30 = 150 > 100 capacity
        BondRecord bond = ingestAndOpen(totalSize);

        String[] users = {"INV-001", "INV-002", "INV-003", "INV-004", "INV-005"};
        int n = users.length;

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger(0);

        List<Future<Response>> futures = new java.util.ArrayList<>();
        for (String user : users) {
            futures.add(pool.submit((Callable<Response>) () -> {
                startGate.await(); // release all threads simultaneously
                return v1.subscribe(bond.isin, user, perOrder);
            }));
        }

        startGate.countDown(); // fire all at once
        for (Future<Response> f : futures) {
            Response r = f.get(30, TimeUnit.SECONDS);
            if (r.statusCode() / 100 == 2) {
                accepted.incrementAndGet();
            }
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        long acceptedQuantity = (long) accepted.get() * perOrder;
        assertTrue(acceptedQuantity <= totalSize,
                "Accepted subscriptions must not exceed capacity. Accepted "
                        + accepted.get() + " orders (" + acceptedQuantity
                        + " units) but totalSize is " + totalSize);
    }
}
