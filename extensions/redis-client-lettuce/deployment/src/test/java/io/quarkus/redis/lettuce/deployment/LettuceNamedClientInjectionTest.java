package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Tests that Lettuce beans can be injected using {@code @RedisClientName} qualifier.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceNamedClientInjectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.my-lettuce.hosts", "${quarkus.redis.tr}");

    @Inject
    @RedisClientName("my-lettuce")
    RedisClient redisClient;

    @Inject
    @RedisClientName("my-lettuce")
    StatefulRedisConnection<String, String> connection;

    @Test
    public void testNamedBeansAreInjected() {
        assertThat(redisClient).isNotNull();
        assertThat(connection).isNotNull();
    }

    @Test
    public void testNamedClientSetAndGet() {
        connection.sync().set("lettuce:named:key", "named-value");
        String value = connection.sync().get("lettuce:named:key");
        assertThat(value).isEqualTo("named-value");
    }
}
