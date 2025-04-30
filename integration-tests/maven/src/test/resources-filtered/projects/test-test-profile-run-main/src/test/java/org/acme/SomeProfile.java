package org.acme;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class SomeProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> configs = new HashMap<>();
        configs.put("test.overridden.value", "sausages");
        return configs;
    }
}
