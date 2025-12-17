package io.quarkus.test.common;

/**
 * @deprecated use {@link RestAssuredStateManager} instead
 */
@Deprecated(forRemoval = true, since = "3.31")
public class RestAssuredURLManager {

    public static void setURL(boolean useSecureConnection) {
        setURL(useSecureConnection, null, null);
    }

    public static void setURL(boolean useSecureConnection, String additionalPath) {
        setURL(useSecureConnection, null, additionalPath);
    }

    public static void setURL(boolean useSecureConnection, Integer port) {
        setURL(useSecureConnection, port, null);
    }

    public static void setURL(boolean useSecureConnection, Integer port, String additionalPath) {
        RestAssuredStateManager.setURL(useSecureConnection, port, additionalPath);
    }

    public static void clearURL() {
        RestAssuredStateManager.clearURL();
    }
}
