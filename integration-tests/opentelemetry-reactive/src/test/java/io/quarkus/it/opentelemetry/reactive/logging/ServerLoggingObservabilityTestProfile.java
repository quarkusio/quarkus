package io.quarkus.it.opentelemetry.reactive.logging;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class ServerLoggingObservabilityTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.rest.logging.scope", "request-response",
                "quarkus.otel.traces.exporter", "none",
                "quarkus.http.access-log.enabled", "true",
                "quarkus.security.users.embedded.users.alice", "alice",
                "quarkus.security.users.embedded.roles.alice", "user",
                "quarkus.http.auth.basic", "true");
    }
}
