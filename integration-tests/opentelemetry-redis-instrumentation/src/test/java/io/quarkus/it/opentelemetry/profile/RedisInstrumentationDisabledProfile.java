package io.quarkus.it.opentelemetry.profile;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class RedisInstrumentationDisabledProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> overrides = new HashMap();
        overrides.put("quarkus.otel.instrument.vertx-redis-client", "false");
        return overrides;

    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
