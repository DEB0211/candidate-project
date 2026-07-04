package com.bis.qa.util;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * Helpers for reading fields from responses whose exact key names/casing are not
 * known ahead of time (API v1 snake_case vs v2 camelCase, per PRODUCT.md
 * section 11). Each accessor tries a list of candidate JSON paths and returns
 * the first that resolves.
 */
public final class JsonFields {

    private JsonFields() {
    }

    public static String firstString(Response response, String... candidatePaths) {
        JsonPath jp = response.jsonPath();
        for (String path : candidatePaths) {
            try {
                String value = jp.getString(path);
                if (value != null) {
                    return value;
                }
            } catch (Exception ignored) {
                // try next candidate
            }
        }
        return null;
    }

    public static Long firstLong(Response response, String... candidatePaths) {
        String value = firstString(response, candidatePaths);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
