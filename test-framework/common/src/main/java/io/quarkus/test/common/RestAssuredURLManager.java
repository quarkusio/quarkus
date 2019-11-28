package io.quarkus.test.common;

import java.lang.reflect.Field;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Utility class that sets the rest assured port to the default test port.
 * <p>
 * This uses reflection so as to not introduce a dependency on rest-assured
 * <p>
 * TODO: do we actually want this here, or should it be in a different module?
 */
public class RestAssuredURLManager {

    private static final int DEFAULT_HTTP_PORT = 8081;
    private static final int DEFAULT_HTTPS_PORT = 8444;

    private static final Field portField;
    private static final Field baseURIField;
    private static final Field basePathField;
    private static int oldPort;
    private static String oldBaseURI;
    private static String oldBasePath;

    static {
        Field p;
        Field baseURI;
        Field basePath;
        try {
            Class<?> restAssured = Class.forName("io.restassured.RestAssured");
            p = restAssured.getField("port");
            p.setAccessible(true);
            baseURI = restAssured.getField("baseURI");
            baseURI.setAccessible(true);
            basePath = restAssured.getField("basePath");
            basePath.setAccessible(true);
        } catch (Exception e) {
            p = null;
            baseURI = null;
            basePath = null;
        }
        portField = p;
        baseURIField = baseURI;
        basePathField = basePath;
    }

    private RestAssuredURLManager() {

    }

    private static int getPortFromConfig(String key, int defaultValue) {
        return ConfigProvider.getConfig().getOptionalValue(key, Integer.class).orElse(defaultValue);
    }

    public static void setURL(boolean useSecureConnection) {
        if (portField != null) {
            try {
                oldPort = (Integer) portField.get(null);
                int port = useSecureConnection ? getPortFromConfig("quarkus.https.test-port", DEFAULT_HTTPS_PORT)
                        : getPortFromConfig("quarkus.http.test-port", DEFAULT_HTTP_PORT);
                portField.set(null, port);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (baseURIField != null) {
            try {
                oldBaseURI = (String) baseURIField.get(null);
                final String protocol = useSecureConnection ? "https://" : "http://";
                String baseURI = protocol + ConfigProvider.getConfig().getOptionalValue("quarkus.http.host", String.class)
                        .orElse("localhost");
                baseURIField.set(null, baseURI);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (basePathField != null) {
            try {
                oldBasePath = (String) basePathField.get(null);
                Optional<String> basePath = ConfigProvider.getConfig().getOptionalValue("quarkus.http.root-path",
                        String.class);
                if (basePath.isPresent()) {
                    basePathField.set(null, basePath.get());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static void clearURL() {
        if (portField != null) {
            try {
                portField.set(null, oldPort);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (baseURIField != null) {
            try {
                baseURIField.set(null, oldBaseURI);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (basePathField != null) {
            try {
                basePathField.set(null, oldBasePath);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
