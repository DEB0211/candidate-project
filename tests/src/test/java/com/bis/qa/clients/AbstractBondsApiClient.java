package com.bis.qa.clients;

import com.bis.qa.config.TestConfig;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Shared behaviour for the versioned Bonds API clients.
 *
 * <p>Endpoint paths follow REST conventions derived from PRODUCT.md sections 6
 * and 11. They are centralised here so that, if Swagger reveals a different
 * shape, only this class and its subclasses need to change - not the tests.
 */
abstract class AbstractBondsApiClient {

    protected final String baseUri = TestConfig.apiBaseUri();

    /** e.g. {@code /api/v1} or {@code /api/v2}. */
    protected abstract String versionPrefix();

    protected RequestSpecification req() {
        return given()
                .baseUri(baseUri)
                .accept("application/json")
                .contentType("application/json");
    }

    protected RequestSpecification reqAs(String userId) {
        RequestSpecification spec = req();
        if (userId != null) {
            spec.header("X-User-Id", userId);
        }
        return spec;
    }

    @Step("List bonds ({0})")
    public Response listBonds() {
        return req().when().get(versionPrefix() + "/bonds").andReturn();
    }

    @Step("Get bond {0}")
    public Response getBond(String isin) {
        return req().when().get(versionPrefix() + "/bonds/{isin}", isin).andReturn();
    }

    @Step("Subscribe user {1} to bond {0} qty {2}")
    public Response subscribe(String isin, String userId, long quantity) {
        return reqAs(userId)
                .body(Map.of("quantity", quantity))
                .when()
                .post(versionPrefix() + "/bonds/{isin}/subscriptions", isin)
                .andReturn();
    }

    /** Subscribe with an arbitrary raw body (for negative validation cases). */
    @Step("Subscribe user {1} to bond {0} with raw body")
    public Response subscribeRaw(String isin, String userId, Object body) {
        RequestSpecification spec = reqAs(userId);
        if (body != null) {
            spec.body(body);
        }
        return spec.when()
                .post(versionPrefix() + "/bonds/{isin}/subscriptions", isin)
                .andReturn();
    }

    @Step("Get portfolio for user {0}")
    public Response getPortfolio(String userId) {
        return reqAs(userId).when().get(versionPrefix() + "/portfolio").andReturn();
    }

    public String versionPrefixPublic() {
        return versionPrefix();
    }
}
