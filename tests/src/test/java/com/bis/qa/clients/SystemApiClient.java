package com.bis.qa.clients;

import com.bis.qa.config.TestConfig;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;

/**
 * Client for the System Control API (PRODUCT.md section 12):
 * {@code /api/system/date}, {@code /api/system/advance-date}, {@code /api/system/reset}.
 */
public class SystemApiClient {

    private final String baseUri;

    public SystemApiClient() {
        this.baseUri = TestConfig.apiBaseUri();
    }

    private RequestSpecification req() {
        return given()
                .baseUri(baseUri)
                .accept("application/json")
                .contentType("application/json");
    }

    @Step("GET current business date")
    public Response getDate() {
        return req().when().get("/api/system/date").andReturn();
    }

    @Step("Read current business date")
    public LocalDate currentBusinessDate() {
        Response r = getDate();
        String value = r.jsonPath().getString("business_date");
        if (value == null) {
            value = r.jsonPath().getString("businessDate");
        }
        return LocalDate.parse(value);
    }

    @Step("POST advance business date by one day")
    public Response advanceDate() {
        return req().when().post("/api/system/advance-date").andReturn();
    }

    /**
     * Advances the business date until it reaches (>=) the target date.
     * Returns the number of advances performed. Guards against runaway loops.
     */
    @Step("Advance business date to on-or-after {target}")
    public int advanceTo(LocalDate target) {
        int advances = 0;
        LocalDate current = currentBusinessDate();
        // Hard cap: never advance more than ~2 years of days in a test.
        int cap = 800;
        while (current.isBefore(target) && advances < cap) {
            advanceDate();
            current = currentBusinessDate();
            advances++;
        }
        return advances;
    }

    @Step("POST reset system to today's date")
    public Response reset() {
        return req().when().post("/api/system/reset").andReturn();
    }

    public static void configureAllureLogging() {
        // Attach request/response details to Allure on failures.
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
