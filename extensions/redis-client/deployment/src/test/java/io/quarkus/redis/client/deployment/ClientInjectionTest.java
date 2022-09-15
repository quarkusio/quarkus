package io.quarkus.redis.client.deployment;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;

@QuarkusTestResource(RedisTestResource.class)
public class ClientInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.my-redis.hosts", "${quarkus.redis.tr}");

    @Inject
    RedisClient legacyClient;

    @Inject
    @RedisClientName("my-redis")
    RedisClient legacyClient2;

    @Inject
    ReactiveRedisClient legacyReactiveClient;

    @Inject
    @RedisClientName("my-redis")
    ReactiveRedisClient legacyReactiveClient2;

    @Inject
    Redis redis;

    @Inject
    @RedisClientName("my-redis")
    Redis myRedis;

    @Inject
    io.vertx.redis.client.Redis bareRedis;

    @Inject
    @RedisClientName("my-redis")
    io.vertx.redis.client.Redis myBareRedis;

    @Inject
    RedisAPI api;

    @Inject
    @RedisClientName("my-redis")
    RedisAPI myapi;

    @Inject
    io.vertx.redis.client.RedisAPI bareRedisAPI;

    @Inject
    @RedisClientName("my-redis")
    io.vertx.redis.client.RedisAPI myBareRedisAPI;

    @Test
    public void testDefault() {
        Assertions.assertThat(redis).isNotNull();
        Assertions.assertThat(bareRedis).isNotNull();
        Assertions.assertThat(api).isNotNull();
        Assertions.assertThat(bareRedisAPI).isNotNull();
        Assertions.assertThat(legacyClient).isNotNull();
        Assertions.assertThat(legacyReactiveClient).isNotNull();
    }

    @Test
    public void testMyRedis() {
        Assertions.assertThat(myRedis).isNotNull();
        Assertions.assertThat(myBareRedis).isNotNull();
        Assertions.assertThat(myapi).isNotNull();
        Assertions.assertThat(myBareRedisAPI).isNotNull();
        Assertions.assertThat(legacyClient2).isNotNull();
        Assertions.assertThat(legacyReactiveClient2).isNotNull();
    }
}
