package com.bis.qa.config;

import java.io.InputStream;
import java.time.ZoneId;
import java.util.Properties;

/**
 * Central test configuration.
 *
 * <p>Resolution order for every key (highest priority first):
 * <ol>
 *   <li>JVM system property ({@code -Dapi.baseUri=...})</li>
 *   <li>Environment variable with the same name</li>
 *   <li>{@code test.properties} on the classpath</li>
 * </ol>
 * This lets the same suite run locally and in CI without code changes.
 */
public final class TestConfig {

    private static final Properties FILE_PROPS = new Properties();

    static {
        try (InputStream in = TestConfig.class.getClassLoader().getResourceAsStream("test.properties")) {
            if (in != null) {
                FILE_PROPS.load(in);
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Unable to load test.properties: " + e.getMessage());
        }
    }

    private TestConfig() {
    }

    public static String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys.trim();
        }
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String fromFile = FILE_PROPS.getProperty(key);
        return fromFile == null ? null : fromFile.trim();
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : value;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    // ----- Convenience accessors -------------------------------------------------

    public static String apiBaseUri() {
        return get("api.baseUri", "http://localhost:8080");
    }

    public static String uiBaseUrl() {
        return get("ui.baseUrl", "http://localhost:5173");
    }

    public static String sftpHost() {
        return get("sftp.host", "localhost");
    }

    public static int sftpPort() {
        return getInt("sftp.port", 2222);
    }

    public static String sftpUsername() {
        return get("sftp.username", "bonduser");
    }

    public static String sftpPassword() {
        return get("sftp.password", "bondpass");
    }

    public static String sftpRemoteDir() {
        return get("sftp.remoteDir", "/upload/bonds");
    }

    public static boolean uiHeadless() {
        return getBoolean("ui.headless", true);
    }

    public static String uiBrowser() {
        return get("ui.browser", "chrome");
    }

    public static int uiTimeoutSeconds() {
        return getInt("ui.timeoutSeconds", 15);
    }

    public static int ingestionPollSeconds() {
        return getInt("ingestion.pollSeconds", 30);
    }

    public static ZoneId businessZone() {
        return ZoneId.of(get("business.timezone", "Asia/Kuala_Lumpur"));
    }
}
