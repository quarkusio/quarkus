package io.quarkus.redis.devservices.it;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.devservices.it.profiles.DevServicesNonUniquePortProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DevServicesNonUniquePortProfile.class)
public class DevServicesRedisNonUniquePortTest {

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

    @Test
    public void shouldHaveStack() {
        // The entertainingly named lolwut command gives a version number but doesn't say if stack is available, so try using some of the commands
        try {
            redisClient.bloom(String.class).bfadd("key", "whatever");
        } catch (Exception e) {
            fail("Redis stack should be available on the connected back end (underlying error was  " + e + ")");
        }
    }

}
