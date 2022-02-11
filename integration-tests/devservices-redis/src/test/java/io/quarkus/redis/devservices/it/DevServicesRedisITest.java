package io.quarkus.redis.devservices.it;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.devservices.it.profiles.DevServiceRedis;
import io.quarkus.redis.devservices.it.utils.SocketKit;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DevServiceRedis.class)
public class DevServicesRedisITest {

    @Inject
    RedisClient redisClient;

    @BeforeEach
    public void setUp() {
        redisClient.set(Arrays.asList("anykey", "anyvalue"));
    }

    @Test
    @DisplayName("given quarkus.redis.hosts disabled should start redis testcontainer")
    public void shouldStartRedisContainer() {
        Assertions.assertTrue(SocketKit.isPortAlreadyUsed(6379));
    }

    @Test
    @DisplayName("given redis container must communicate with it and return value by key")
    public void shouldReturnAllKeys() {
        Assertions.assertEquals("anyvalue", redisClient.get("anykey").toString());
    }

}
