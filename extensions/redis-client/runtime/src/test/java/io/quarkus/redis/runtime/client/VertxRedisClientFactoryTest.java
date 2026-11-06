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

    @Test
    void shouldApplyQueryWithValkeyUri() {
        String appliedValkey = VertxRedisClientFactory.applyClientQueryParam(
                "quarkus-app", URI.create("valkey://localhost:6379"));
        Assertions.assertThat(appliedValkey).isEqualTo("valkey://localhost:6379?client=quarkus-app");

        String appliedValkeys = VertxRedisClientFactory.applyClientQueryParam(
                "quarkus-app", URI.create("valkeys://localhost:6379"));
        Assertions.assertThat(appliedValkeys).isEqualTo("valkeys://localhost:6379?client=quarkus-app");
    }

    @Test
    void shouldRecognizeSecureSchemes() {
        Assertions.assertThat(VertxRedisClientFactory.isSecureScheme("rediss")).isTrue();
        Assertions.assertThat(VertxRedisClientFactory.isSecureScheme("REDISS")).isTrue();
        Assertions.assertThat(VertxRedisClientFactory.isSecureScheme("valkeys")).isTrue();
        Assertions.assertThat(VertxRedisClientFactory.isSecureScheme("VALKEYS")).isTrue();
        Assertions.assertThat(VertxRedisClientFactory.isSecureScheme("redis")).isFalse();
        Assertions.assertThat(VertxRedisClientFactory.isSecureScheme("valkey")).isFalse();
        Assertions.assertThat(VertxRedisClientFactory.isSecureScheme("unix")).isFalse();
        Assertions.assertThat(VertxRedisClientFactory.isSecureScheme(null)).isFalse();
    }

    @Test
    void shouldApplyUsernameAndPasswordWithValkeyHosts() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withConverter(Duration.class, 100, new DurationConverter())
                .withMapping(RedisConfig.class)
                .withDefaultValues(Map.of(
                        "quarkus.redis.hosts", "valkey://localhost:6379",
                        "quarkus.redis.username", "valkey-user",
                        "quarkus.redis.password", "valkey-secret",
                        "quarkus.redis.secure-valkey.hosts", "valkeys://localhost:6380",
                        "quarkus.redis.secure-valkey.password", "valkeys-secret"))
                .build();
        Map<String, RedisClientConfig> clients = config.getConfigMapping(RedisConfig.class).clients();

        RedisOptions options = new RedisOptions();
        VertxRedisClientFactory.configureCredentials(options, clients.get(RedisConfig.DEFAULT_CLIENT_NAME));
        Assertions.assertThat(options.getUser()).isEqualTo("valkey-user");
        Assertions.assertThat(options.getPassword()).isEqualTo("valkey-secret");

        options = new RedisOptions();
        VertxRedisClientFactory.configureCredentials(options, clients.get("secure-valkey"));
        Assertions.assertThat(options.getUser()).isNull();
        Assertions.assertThat(options.getPassword()).isEqualTo("valkeys-secret");
    }

    @Test
    void shouldSupportValkeyConnectionStringInRedisOptions() {
        RedisOptions options = new RedisOptions();
        options.addConnectionString("valkey://localhost:6379/1");
        options.addConnectionString("valkeys://secure-host:6380/2");

        Assertions.assertThat(options.getEndpoints()).hasSize(2);
        Assertions.assertThat(options.getEndpoints().get(0)).contains("6379");
        Assertions.assertThat(options.getEndpoints().get(1)).contains("6380");
    }

}
