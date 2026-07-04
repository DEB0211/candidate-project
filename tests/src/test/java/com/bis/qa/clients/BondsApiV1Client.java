package com.bis.qa.clients;

/**
 * API v1 client (PRODUCT.md section 11).
 *
 * <p>v1 uses <b>snake_case</b> field names and a flat response structure, e.g.
 * {@code face_value}, {@code coupon_rate}, {@code book_open_date}, {@code total_size}.
 */
public class BondsApiV1Client extends AbstractBondsApiClient {

    @Override
    protected String versionPrefix() {
        return "/api/v1";
    }
}
