package io.quarkus.redis.devservices.it;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PlainQuarkusTest {

    @Inject
    RedisDataSource redisClient;

    @Test
    public void shouldStartRedisContainer() {
        assertNotNull(redisClient);
    }
}
