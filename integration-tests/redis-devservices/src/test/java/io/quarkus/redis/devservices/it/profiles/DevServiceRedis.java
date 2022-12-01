package io.quarkus.redis.devservices.it.profiles;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DevServiceRedis implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Collections.singletonMap("quarkus.redis.devservices.port", "6379");
    }
}
