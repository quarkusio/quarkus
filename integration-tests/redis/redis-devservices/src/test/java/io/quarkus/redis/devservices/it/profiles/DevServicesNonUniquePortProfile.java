package io.quarkus.redis.devservices.it.profiles;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DevServicesNonUniquePortProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        // Set an image, both for coverage, and so we know we're connecting to the right service
        Map<String, String> overrides = new HashMap<>();
        overrides.put("quarkus.redis.devservices.port", "6371");
        // Choose an image which includes redis stack, so we can identify we're running on it
        overrides.put("quarkus.redis.devservices.image-name", "redis/redis-stack:latest");
        return overrides;
    }
}
