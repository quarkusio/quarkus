package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisClient;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that a {@code hosts-provider-name} matching no {@code RedisHostsProvider} bean fails the startup with the
 * message the Vert.x client gives. The client is requested through a Lettuce type only, so no Vert.x client exists for
 * it and the failure is the Lettuce backend's.
 */
public class LettuceHostsProviderMissingTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.redis.hosts-provider-name", "nowhere")
            // the application's classes live in the Quarkus runtime class loader: compare the class by name
            .assertException(e -> assertThat(e)
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(ConfigurationException.class.getName()))
                    .hasMessageContaining("Unable to find redis host provider identified with nowhere"));

    @Inject
    RedisClient redisClient;

    @Test
    void shouldNotRun() {
        // the application fails to start before this runs
    }
}
