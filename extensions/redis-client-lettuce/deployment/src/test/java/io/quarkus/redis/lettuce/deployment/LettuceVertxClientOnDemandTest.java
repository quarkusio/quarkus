package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Request;

/**
 * Verifies that, with the Lettuce backend, the Vert.x Redis client is still created for a client injected through a
 * Vert.x Redis client type, and only for that client: the data sources of every client are served by Lettuce.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceVertxClientOnDemandTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(VertxClientBeans.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.low-level.hosts", "${quarkus.redis.tr}");

    @Inject
    RedisDataSource ds;

    @Inject
    @RedisClientName("low-level")
    RedisDataSource lowLevelDs;

    @Inject
    @RedisClientName("low-level")
    Redis lowLevel;

    @Test
    void theVertxClientIsCreatedForTheClientInjectingIt() {
        assertThat(VertxClientBeans.clientNames()).containsExactly("low-level");
        assertThat(lowLevel.send(Request.cmd(Command.PING)).await().atMost(Duration.ofSeconds(10)).toString())
                .isEqualTo("PONG");
    }

    @Test
    void theDataSourcesWork() {
        assertThat(ds.execute("PING").toString()).isEqualTo("PONG");
        assertThat(lowLevelDs.execute("PING").toString()).isEqualTo("PONG");
    }
}
