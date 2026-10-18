package io.quarkus.it.opentelemetry.vertx.exporter;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public final class SimpleProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.otel.simple", "true");
    }
}
