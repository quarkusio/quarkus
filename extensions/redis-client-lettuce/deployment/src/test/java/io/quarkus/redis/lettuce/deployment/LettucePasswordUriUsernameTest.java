package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that a lone user info in the URI is the user name, as with the Vert.x client (Lettuce alone would read
 * it as the password): {@code redis://default@host} combined with {@code quarkus.redis.password} authenticates as
 * the {@code default} user with the configured password.
 */
@QuarkusTestResource(RedisPasswordTestResource.class)
public class LettucePasswordUriUsernameTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts",
                    "redis://default@${redis.requirepass.host}:${redis.requirepass.port}")
            .overrideConfigKey("quarkus.redis.password", "${redis.requirepass.password}");

    @Inject
    RedisDataSource ds;

    @Test
    void userNameFromUriPasswordFromProperty() {
        assertThat(ds.execute("PING").toString()).isEqualTo("PONG");
    }
}
