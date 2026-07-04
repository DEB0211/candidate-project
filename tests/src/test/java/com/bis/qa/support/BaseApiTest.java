package com.bis.qa.support;

import com.bis.qa.clients.BondsApiV1Client;
import com.bis.qa.clients.BondsApiV2Client;
import com.bis.qa.clients.SystemApiClient;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.HttpClientConfig;
import org.testng.annotations.BeforeClass;

/**
 * Base class for all API-layer tests. Provides configured clients and sensible
 * RestAssured defaults (timeouts, failure logging).
 */
public abstract class BaseApiTest {

    protected final SystemApiClient system = new SystemApiClient();
    protected final BondsApiV1Client v1 = new BondsApiV1Client();
    protected final BondsApiV2Client v2 = new BondsApiV2Client();

    @BeforeClass(alwaysRun = true)
    public void configureRestAssured() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.config = RestAssuredConfig.config().httpClient(
                HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 10_000)
                        .setParam("http.socket.timeout", 20_000));
    }
}
