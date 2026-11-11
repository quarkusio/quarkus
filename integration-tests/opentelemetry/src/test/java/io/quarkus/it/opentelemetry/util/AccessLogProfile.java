package io.quarkus.it.opentelemetry.util;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class AccessLogProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.http.access-log.enabled", "true",
                "quarkus.http.access-log.pattern",
                "%{REMOTE_HOST} %{REQUEST_LINE} traceId=%{X,traceId} spanId=%{X,spanId}");
    }
}
