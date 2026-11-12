package io.quarkus.redis.lettuce.deployment.sentinel;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.lettuce.deployment.RedisSentinelTestResource;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * {@code role=sentinel} connects the Vert.x client to a sentinel node itself; a sentinel has no data commands, so
 * the Lettuce backend rejects it at startup with a message naming the supported roles.
 */
@QuarkusTestResource(RedisSentinelTestResource.class)
public class LettuceSentinelRoleRejectedTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${redis.sentinel.hosts}")
            .overrideConfigKey("quarkus.redis.client-type", "sentinel")
            .overrideConfigKey("quarkus.redis.master-name", "${redis.sentinel.master-name}")
            .overrideConfigKey("quarkus.redis.role", "sentinel")
            .assertException(e -> assertThat(e)
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(ConfigurationException.class.getName()))
                    .hasMessageContainingAll("quarkus.redis.role=sentinel", "'master' or 'replica'"));

    /** Requests the client, so that the recorder configures it (and rejects the role) at startup. */
    @Inject
    RedisDataSource blocking;

    @Test
    void shouldNotRun() {
        // the application fails to start before this runs
    }
}
