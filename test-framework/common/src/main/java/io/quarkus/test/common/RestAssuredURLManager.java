package io.quarkus.test.common;

import java.lang.reflect.Field;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Utility class that sets the rest assured port to the default test port.
 * <p>
 * This uses reflection so as to not introduce a dependency on rest-assured
 * <p>
 * TODO: do we actually want this here, or should it be in a different module?
 */
public class RestAssuredURLManager {

    private static final Field portField;
    private static final Field baseURIField;
    private static int oldPort;
    private static String oldBaseURI;

    static {
        Field p;
        Field base;
        try {
            Class<?> restAssured = Class.forName("io.restassured.RestAssured");
            p = restAssured.getField("port");
            p.setAccessible(true);
            base = restAssured.getField("baseURI");
            base.setAccessible(true);
        } catch (Exception e) {
            p = null;
            base = null;
        }
        portField = p;
        baseURIField = base;
    }

    public static void setURL() {
        if (portField != null) {
            try {
                oldPort = (Integer) portField.get(null);
                int port = ConfigProvider.getConfig().getOptionalValue("quarkus.http.test-port", Integer.class).orElse(8081);
                portField.set(null, port);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (baseURIField != null) {
            try {
                oldBaseURI = (String) baseURIField.get(null);
                String baseURI = "http://"
                        + ConfigProvider.getConfig().getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
                baseURIField.set(null, baseURI);
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
    }
}
