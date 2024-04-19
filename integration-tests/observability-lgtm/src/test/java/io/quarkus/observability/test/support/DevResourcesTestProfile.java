package io.quarkus.observability.test.support;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DevResourcesTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.observability.dev-resources", "true",
                "quarkus.observability.enabled", "false");
    }
}
