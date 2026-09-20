package io.quarkus.it.extension;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ConfigManager implements QuarkusTestResourceLifecycleManager {
    public final static String TEST_PROPERTY_NAME = "test.property";
    public final static String TEST_PROPERTY_VALUE = "value";

    @Override
    public Map<String, String> start() {
        return Map.of(TEST_PROPERTY_NAME, TEST_PROPERTY_VALUE);
    }

    @Override
    public void stop() {

    }
}
