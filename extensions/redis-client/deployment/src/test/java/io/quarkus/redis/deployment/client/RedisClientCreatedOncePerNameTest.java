package io.quarkus.redis.deployment.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.client.RedisOptionsCustomizer;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.redis.client.RedisOptions;

/**
 * Injecting a data source makes the client build step and the data source build step request the same
 * Redis client, so the recorder is asked to initialize it twice. It must still build the underlying
 * Vert.x client only once per client name.
 */
public class RedisClientCreatedOncePerNameTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(CreationCounter.class))
            .overrideConfigKey("quarkus.redis.hosts", "redis://localhost:6379")
            .overrideConfigKey("quarkus.redis.second.hosts", "redis://localhost:6379")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false");

    // These injection points are what makes both build steps request the clients.
    @Inject
    RedisDataSource defaultDataSource;

    @Inject
    @RedisClientName("second")
    ReactiveRedisDataSource secondDataSource;

    @Inject
    CreationCounter counter;

    @Test
    public void eachClientShouldBeCreatedOnce() {
        assertThat(counter.getCreations())
                .containsOnly(entry(RedisConfig.DEFAULT_CLIENT_NAME, 1), entry("second", 1));
    }

    /**
     * The customizer is invoked once per created Vert.x client, which makes it a convenient way to
     * count the clients built for a given name. It must stay normal-scoped so the instance ArC hands
     * the client factory is the one injected here.
     */
    @ApplicationScoped
    public static class CreationCounter implements RedisOptionsCustomizer {

        private final Map<String, Integer> creations = new ConcurrentHashMap<>();

        @Override
        public void customize(String clientName, RedisOptions options) {
            this.creations.merge(clientName, 1, Integer::sum);
        }

        public Map<String, Integer> getCreations() {
            return this.creations;
        }
    }
}
