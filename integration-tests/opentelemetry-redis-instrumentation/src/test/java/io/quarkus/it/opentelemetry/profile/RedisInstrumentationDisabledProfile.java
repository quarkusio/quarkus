package io.quarkus.it.opentelemetry.profile;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class RedisInstrumentationDisabledProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Collections.singletonMap("quarkus.otel.instrument.vertx-redis-client", "false");
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
