package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.lettuce.core.RedisConnectionException;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.DockerImageNames;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that the data sources of the Lettuce backend connect with their first command, not at startup, as the
 * Vert.x data sources do: the application starts although nothing listens on the configured port, the commands sent
 * meanwhile fail with a connection failure, and the ones sent once the server is there connect and succeed.
 */
@QuarkusTestResource(FreePortTestResource.class)
public class LettuceLazyConnectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "redis://127.0.0.1:${lettuce.test.free-port}");

    @Inject
    RedisDataSource ds;

    @Inject
    ReactiveRedisDataSource reactive;

    @Test
    void theApplicationStartsWithoutTheServerAndConnectsOnceItIsThere() {
        // the application started (this test runs) and nothing listens: every command fails, each after its own
        // connection attempt
        assertThatThrownBy(() -> ds.execute("PING")).isInstanceOf(RedisConnectionException.class);
        assertThatThrownBy(() -> reactive.value(String.class).get("lazy").await().atMost(Duration.ofSeconds(10)))
                .isInstanceOf(RedisConnectionException.class);
        assertThatThrownBy(() -> ds.withTransaction(tx -> tx.value(String.class).set("lazy", "tx")))
                .isInstanceOf(RedisConnectionException.class);

        int port = ConfigProvider.getConfig().getValue("lettuce.test.free-port", Integer.class);
        try (GenericContainer<?> server = new GenericContainer<>(
                DockerImageName.parse(DockerImageNames.getImage("redis.image"))).withExposedPorts(6379)) {
            // published on the port the application is configured with
            server.setPortBindings(List.of(port + ":6379"));
            server.start();

            assertThat(ds.execute("PING").toString()).isEqualTo("PONG");
            ds.value(String.class).set("lazy", "value");
            assertThat(reactive.value(String.class).get("lazy").await().atMost(Duration.ofSeconds(10)))
                    .isEqualTo("value");
            TransactionResult result = ds.withTransaction(tx -> tx.value(String.class).set("lazy", "tx"));
            assertThat(result.discarded()).isFalse();
            assertThat(ds.value(String.class).get("lazy")).isEqualTo("tx");
        }
    }

}
