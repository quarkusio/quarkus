package io.quarkus.redis.devservices.it;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.devservices.it.profiles.DevServicesRandomPortProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DevServicesRandomPortProfile.class)
public class DevServicesRedisRandomPortITest {

    @Inject
    RedisDataSource redisClient;

    @BeforeEach
    public void setUp() {
        redisClient.value(String.class).set("anykey", "anyvalue");
    }

    @Test
    @DisplayName("given redis container must communicate with it and return value by key")
    public void shouldReturnAllKeys() {
        Assertions.assertEquals("anyvalue", redisClient.value(String.class).get("anykey").toString());
    }

}
