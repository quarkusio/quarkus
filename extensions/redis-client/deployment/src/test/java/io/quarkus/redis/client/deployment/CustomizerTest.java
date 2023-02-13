package io.quarkus.redis.client.deployment;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.client.RedisOptionsCustomizer;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;

@QuarkusTestResource(RedisTestResource.class)
public class CustomizerTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(MyCustomizer.class))
            .overrideConfigKey("quarkus.redis.hosts", "redis://wont-work")
            .overrideConfigKey("quarkus.redis.my-redis.hosts", "redis://wont-work");

    @Inject
    RedisAPI api;

    @Inject
    @RedisClientName("my-redis")
    RedisAPI myapi;

    @Test
    public void testCustomization() {
        String key = UUID.randomUUID().toString();
        api.setAndAwait(List.of(key, "test-" + key));
        String v = myapi.getAndAwait(key).toString();
        Assertions.assertThat(v).isEqualTo("test-" + key);
    }

    @ApplicationScoped
    public static class MyCustomizer implements RedisOptionsCustomizer {

        @Override
        public void customize(String clientName, RedisOptions options) {
            String v = ConfigProviderResolver.instance().getConfig().getValue("quarkus.redis.tr", String.class);
            if (clientName.equalsIgnoreCase("my-redis")
                    || clientName.equalsIgnoreCase(RedisConfig.DEFAULT_CLIENT_NAME)) {
                options.setEndpoints(List.of(v));
            } else {
                throw new IllegalStateException("Unknown client name: " + clientName);
            }
        }
    }
}
