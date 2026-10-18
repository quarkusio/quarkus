package io.quarkus.redis.runtime.client;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.vertx.redis.client.RedisOptions;

class VertxRedisClientFactoryTest {

    @Test
    void shouldApplyQuery() {

        String applied = VertxRedisClientFactory.applyClientQueryParam(
                "quarkus-app", URI.create("redis://localhost:6379"));
        Assertions.assertThat(applied).isEqualTo("redis://localhost:6379?client=quarkus-app");
    }

    @Test
    void shouldNotApplyQuery() {
        String applied = VertxRedisClientFactory.applyClientQueryParam(
                "quarkus-app", URI.create("redis://localhost:6379?client=quarkiverse-app"));
        Assertions.assertThat(applied).isEqualTo("redis://localhost:6379?client=quarkiverse-app");
    }

    @Test
    void shouldApplyWithReservedURICharacters() {
        String applied = VertxRedisClientFactory.applyClientQueryParam(
                "quarkus&%$ app", URI.create("redis://localhost:6379"));
        Assertions.assertThat(applied).isEqualTo("redis://localhost:6379?client=quarkus&%25$%20app");
    }

    @Test
    void shouldApplyWithQueryParams() {
        String applied = VertxRedisClientFactory.applyClientQueryParam(
                "quarkus-app", URI.create("redis://localhost:6379?someQueryParam=123456789"));
        Assertions.assertThat(applied).isEqualTo("redis://localhost:6379?someQueryParam=123456789&client=quarkus-app");
    }

    @Test
    void shouldApplyUsernameAndPassword() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withConverter(Duration.class, 100, new DurationConverter())
                .withMapping(RedisConfig.class)
                .withDefaultValues(Map.of(
                        "quarkus.redis.hosts", "redis://localhost:6379",
                        "quarkus.redis.username", "alice",
                        "quarkus.redis.password", "secret",
                        "quarkus.redis.other.hosts", "redis://localhost:6380",
                        "quarkus.redis.other.password", "other-secret"))
                .build();
        Map<String, RedisClientConfig> clients = config.getConfigMapping(RedisConfig.class).clients();

        RedisOptions options = new RedisOptions();
        VertxRedisClientFactory.configureCredentials(options, clients.get(RedisConfig.DEFAULT_CLIENT_NAME));
        Assertions.assertThat(options.getUser()).isEqualTo("alice");
        Assertions.assertThat(options.getPassword()).isEqualTo("secret");

        options = new RedisOptions();
        VertxRedisClientFactory.configureCredentials(options, clients.get("other"));
        Assertions.assertThat(options.getUser()).isNull();
        Assertions.assertThat(options.getPassword()).isEqualTo("other-secret");
    }

}
