package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that, like with the Vert.x Redis client, a password encoded in the URI takes precedence over
 * {@code quarkus.redis.password}: the URI carries the right password while the property is wrong.
 */
@QuarkusTestResource(RedisPasswordTestResource.class)
public class LettucePasswordUriPrecedenceTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts",
                    "redis://:${redis.requirepass.password}@${redis.requirepass.host}:${redis.requirepass.port}")
            .overrideConfigKey("quarkus.redis.password", "not-the-password");

    @Inject
    RedisDataSource ds;

    @Test
    void passwordFromUriWins() {
        assertThat(ds.execute("PING").toString()).isEqualTo("PONG");
    }
}
