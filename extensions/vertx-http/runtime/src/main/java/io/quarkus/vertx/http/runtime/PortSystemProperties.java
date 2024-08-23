package io.quarkus.vertx.http.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.quarkus.runtime.LaunchMode;

public class PortSystemProperties {
    private final Map<String, String> portPropertiesToRestore = new HashMap<>();

    public void set(String subProperty, int actualPort, LaunchMode launchMode) {
        String portPropertyValue = String.valueOf(actualPort);
        String portPropertyName = "quarkus." + subProperty + ".port";
        String testPropName = "quarkus." + subProperty + ".test-port";

        set(portPropertyName, testPropName, portPropertyValue, launchMode);
        //if subProperty is "https", the correct properties are not quarkus.https.port and quarkus.https.test-port
        //but quarkus.http.ssl-port and quarkus.http.test-ssl-port
        //the incorrect properties are still set for backward compatibility with code that works around the incorrect
        //names
        if ("https".equals(subProperty)) {
            set("quarkus.http.ssl-port", "quarkus.http.test-ssl-port", portPropertyValue, launchMode);
        }
    }

    private void set(String portPropertyName, String testPropName, String portPropertyValue, LaunchMode launchMode) {
        //we always set the .port property, even if we are in test mode, so this will always
        //reflect the current port
        set(portPropertyName, portPropertyValue);
        if (launchMode == LaunchMode.TEST) {
            //we also set the test-port property in a test
            set(testPropName, portPropertyValue);
        }
        if (launchMode.isDevOrTest()) {
            // set the profile property as well to make sure we don't have any inconsistencies
            portPropertyName = "%" + launchMode.getDefaultProfile() + "." + portPropertyName;
            set(portPropertyName, portPropertyValue);
        }
    }

    private void set(String propertyName, String propertyValue) {
        String prevPropertyValue = System.setProperty(propertyName, propertyValue);
        if (!Objects.equals(prevPropertyValue, propertyValue)) {
            portPropertiesToRestore.put(propertyName, prevPropertyValue);
        }
    }

    public void restore() {
        portPropertiesToRestore.forEach((key, value) -> {
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        });
        portPropertiesToRestore.clear();
    }
}
