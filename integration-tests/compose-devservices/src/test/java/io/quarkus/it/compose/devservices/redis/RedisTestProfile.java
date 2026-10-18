package io.quarkus.it.compose.devservices.redis;

import io.quarkus.test.junit.QuarkusTestProfile;

public class RedisTestProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "redis";
    }
}
