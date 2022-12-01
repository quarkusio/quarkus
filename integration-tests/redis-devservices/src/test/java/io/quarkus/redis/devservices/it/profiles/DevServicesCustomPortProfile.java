package io.quarkus.redis.devservices.it.profiles;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DevServicesCustomPortProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Collections.singletonMap("quarkus.redis.devservices.port", "6371");
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
