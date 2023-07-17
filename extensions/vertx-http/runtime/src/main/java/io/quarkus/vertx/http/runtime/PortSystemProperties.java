package io.quarkus.vertx.http.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.quarkus.runtime.LaunchMode;

public class PortSystemProperties {
    private final Map<String, String> portPropertiesToRestore = new HashMap<>();

    public void set(String subProperty, int actualPort, LaunchMode launchMode) {
        String portPropertyValue = String.valueOf(actualPort);
        //we always set the .port property, even if we are in test mode, so this will always
        //reflect the current port
        String portPropertyName = "quarkus." + subProperty + ".port";
        String prevPortPropertyValue = System.setProperty(portPropertyName, portPropertyValue);
        if (!Objects.equals(prevPortPropertyValue, portPropertyValue)) {
            portPropertiesToRestore.put(portPropertyName, prevPortPropertyValue);
        }
        if (launchMode == LaunchMode.TEST) {
            //we also set the test-port property in a test
            String testPropName = "quarkus." + subProperty + ".test-port";
            String prevTestPropPrevValue = System.setProperty(testPropName, portPropertyValue);
            if (!Objects.equals(prevTestPropPrevValue, portPropertyValue)) {
                portPropertiesToRestore.put(testPropName, prevTestPropPrevValue);
            }
        }
        if (launchMode.isDevOrTest()) {
            // set the profile property as well to make sure we don't have any inconsistencies
            portPropertyName = "%" + launchMode.getDefaultProfile() + "." + portPropertyName;
            prevPortPropertyValue = System.setProperty(portPropertyName, portPropertyValue);
            if (!Objects.equals(prevPortPropertyValue, portPropertyValue)) {
                portPropertiesToRestore.put(portPropertyName, prevPortPropertyValue);
            }
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
