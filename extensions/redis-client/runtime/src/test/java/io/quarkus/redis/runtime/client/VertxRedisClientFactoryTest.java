package io.quarkus.redis.runtime.client;

import java.net.URI;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

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

}
