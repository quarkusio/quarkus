package io.quarkus.it.opentelemetry.util;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class EndUserProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.otel.traces.eusp.enabled", "true");
    }

}
