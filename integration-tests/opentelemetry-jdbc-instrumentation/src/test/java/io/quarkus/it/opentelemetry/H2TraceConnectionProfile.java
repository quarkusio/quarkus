package io.quarkus.it.opentelemetry;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class H2TraceConnectionProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.datasource.h2.jdbc.telemetry.trace-connection", "true");
    }
}
