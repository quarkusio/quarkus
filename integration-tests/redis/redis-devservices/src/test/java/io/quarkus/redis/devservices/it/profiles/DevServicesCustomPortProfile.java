package io.quarkus.redis.devservices.it.profiles;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DevServicesCustomPortProfile implements QuarkusTestProfile {

    public static final String PORT = "6371";

    @Override
    public Map<String, String> getConfigOverrides() {
        return Collections.singletonMap("quarkus.redis.devservices.port", PORT);
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
