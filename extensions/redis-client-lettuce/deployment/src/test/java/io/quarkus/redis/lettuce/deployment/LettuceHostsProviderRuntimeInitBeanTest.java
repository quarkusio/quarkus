package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisClient;
import io.quarkus.redis.client.RedisHostsProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;

class LettuceHostsProviderRuntimeInitBeanTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(VertxInjectingHostsProvider.class))
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.redis.hosts-provider-name", "vertx-injecting")
            .assertException(e -> assertThat(e)
                    .hasStackTraceContaining("Synthetic bean instance for io.vertx.core.Vertx not initialized yet"));

    @Inject
    RedisClient redisClient;

    @Test
    void shouldNotRun() {
        // the application fails to start before this runs
    }

    @ApplicationScoped
    @Identifier("vertx-injecting")
    public static class VertxInjectingHostsProvider implements RedisHostsProvider {

        @Inject
        Vertx vertx;

        @Override
        public Set<URI> getHosts() {
            return Set.of(URI.create("redis://localhost:1"));
        }
    }
}
