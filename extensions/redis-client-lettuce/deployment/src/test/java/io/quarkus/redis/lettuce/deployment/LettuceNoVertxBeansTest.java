package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisClient;
import io.quarkus.arc.Arc;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that an application injecting only Lettuce beans does not cause the Vert.x Redis client
 * stack to be produced as well. Injecting a Lettuce type must not silently create a parallel Vert.x
 * Redis client or a Vert.x-backed {@link RedisDataSource}.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceNoVertxBeansTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}");

    @Inject
    RedisClient lettuceClient;

    @Test
    public void lettuceBeanIsPresent() {
        assertThat(lettuceClient).isNotNull();
    }

    @Test
    public void noVertxRedisClientBeansAreProduced() {
        assertThat(Arc.container().instance(io.vertx.mutiny.redis.client.Redis.class).isAvailable())
                .as("Vert.x Mutiny Redis bean must not be produced for a Lettuce-only application")
                .isFalse();
        assertThat(Arc.container().instance(io.vertx.redis.client.Redis.class).isAvailable())
                .as("Vert.x bare Redis bean must not be produced for a Lettuce-only application")
                .isFalse();
    }

    @Test
    public void noVertxBackedDataSourceBeansAreProduced() {
        assertThat(Arc.container().instance(RedisDataSource.class).isAvailable())
                .as("Vert.x-backed RedisDataSource must not be produced for a Lettuce-only application")
                .isFalse();
        assertThat(Arc.container().instance(ReactiveRedisDataSource.class).isAvailable())
                .as("Vert.x-backed ReactiveRedisDataSource must not be produced for a Lettuce-only application")
                .isFalse();
    }
}
