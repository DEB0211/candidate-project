package com.bis.qa.clients;

/**
 * API v2 client (PRODUCT.md section 11).
 *
 * <p>v2 uses <b>camelCase</b> field names, nested structures, string amounts and
 * ISO timestamps, e.g. {@code faceValue}, {@code couponRate}, nested
 * {@code book: { openDate, closeDate }}.
 */
public class BondsApiV2Client extends AbstractBondsApiClient {

    @Override
    protected String versionPrefix() {
        return "/api/v2";
    }
}
