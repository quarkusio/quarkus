package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that {@code quarkus.redis.password} is applied by the Lettuce backend: the server requires a password and
 * the URI does not carry one.
 */
@QuarkusTestResource(RedisPasswordTestResource.class)
public class LettucePasswordTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${redis.requirepass.uri}")
            .overrideConfigKey("quarkus.redis.password", "${redis.requirepass.password}");

    @Inject
    RedisDataSource ds;

    @Test
    void passwordFromPropertyIsUsed() {
        assertThat(ds.execute("PING").toString()).isEqualTo("PONG");
        ds.value(String.class).set("lettuce:password:key", "value");
        assertThat(ds.value(String.class).get("lettuce:password:key")).isEqualTo("value");
    }
}
