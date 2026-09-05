package io.quarkus.it.observation.micrometer.opentelemetry;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class CustomizationProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("test.observation.customizations", "true");
    }
}
