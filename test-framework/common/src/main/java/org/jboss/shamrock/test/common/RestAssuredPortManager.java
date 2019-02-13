package org.jboss.shamrock.test.common;

import java.lang.reflect.Field;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Utility class that sets the rest assured port to the default test port.
 * <p>
 * This uses reflection so as to not introduce a dependency on rest-assured
 * <p>
 * TODO: do we actually want this here, or should it be in a different module?
 */
public class RestAssuredPortManager {

    private static final Field portField;
    private static int oldPort;

    static  {
        Field p;
        try {
            Class<?> restAssured = Class.forName("io.restassured.RestAssured");
            p = restAssured.getField("port");
            p.setAccessible(true);
        } catch (Exception e) {
            p = null;
        }
        portField = p;
    }

    public static void setPort() {
        if (portField != null) {
            try {
                oldPort = (Integer) portField.get(null);
                int port = ConfigProvider.getConfig().getOptionalValue("shamrock.http.test-port", Integer.class).orElse(8081);
                portField.set(null, port);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static void clearPort() {
        if (portField != null) {
            try {
                portField.set(null, oldPort);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
