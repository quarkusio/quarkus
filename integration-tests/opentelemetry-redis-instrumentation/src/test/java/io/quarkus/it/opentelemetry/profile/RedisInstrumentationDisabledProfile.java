package io.quarkus.it.opentelemetry.profile;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class RedisInstrumentationDisabledProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map overrides = new HashMap();
        overrides.put("quarkus.otel.instrument.vertx-redis-client", "false");
        overrides.put("quarkus.redis.devservices.port", "4001"); // Workaround for #45785; the dev services for distinct dev services cannot share a port
        return overrides;

    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
