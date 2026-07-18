package io.quarkus.it.micrometer.prometheus;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class OtelOffProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>(Map.of(
                "quarkus.otel.enabled", "false"));
        return config;
    }
}
