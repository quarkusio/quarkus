package io.quarkus.redis.lettuce.deployment.sentinel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands.RedisSubscriber;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.lettuce.deployment.RedisSentinelTestResource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * The Lettuce data sources on a Sentinel-managed master ({@code client-type=sentinel}): the configured hosts are the
 * sentinels, the connection goes to the master they monitor, and everything a standalone connection supports
 * (transactions, {@code SELECT}, scoped connections) works on it.
 */
@QuarkusTestResource(RedisSentinelTestResource.class)
public class LettuceSentinelDataSourceTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            // the test runs inside the application: its helper must be part of the archive
            .withApplicationRoot(jar -> jar.addClass(MasterReplicaNodes.class))
            .overrideConfigKey("quarkus.redis.hosts", "${redis.sentinel.hosts}")
            .overrideConfigKey("quarkus.redis.client-type", "sentinel")
            .overrideConfigKey("quarkus.redis.master-name", "${redis.sentinel.master-name}");

    private static MasterReplicaNodes nodes;

    @Inject
    RedisDataSource blocking;

    @Inject
    ReactiveRedisDataSource reactive;

    @ConfigProperty(name = "redis.replication.hosts")
    String replicationHosts;

    @ConfigProperty(name = "redis.sentinel.hosts")
    String sentinelHosts;

    @AfterAll
    static void closeNodes() {
        if (nodes != null) {
            nodes.close();
        }
    }

    private MasterReplicaNodes nodes() {
        if (nodes == null) {
            nodes = new MasterReplicaNodes(replicationHosts, sentinelHosts, RedisSentinelTestResource.MASTER_NAME);
        }
        return nodes;
    }

    @Test
    void connectsToTheMasterTheSentinelsMonitor() {
        assertThat(blocking.execute("PING").toString()).isEqualTo("PONG");
        assertThat(reactive.execute("PING").await().atMost(Duration.ofSeconds(5)).toString()).isEqualTo("PONG");
        // the node the data source talks to is the master the sentinel reports
        assertThat(blocking.execute("INFO", "replication").toString()).contains("role:master");
        assertThat(blocking.execute("CONFIG", "GET", "port").get(1).toInteger()).isEqualTo(nodes().masterPort());
    }

    @Test
    void readsAndWritesOnTheMaster() {
        ValueCommands<String, String> values = blocking.value(String.class);
        values.set("sentinel:key", "v");
        assertThat(values.get("sentinel:key")).isEqualTo("v");
        assertThat(reactive.value(String.class).get("sentinel:key").await().atMost(Duration.ofSeconds(5))).isEqualTo("v");
    }

    @Test
    void supportsTransactionsAndScopedConnections() {
        TransactionResult result = blocking.withTransaction(tx -> {
            tx.value(String.class).set("sentinel:tx", "a");
            tx.value(String.class).get("sentinel:tx");
        });
        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(2);
        assertThat((String) result.get(1)).isEqualTo("a");

        blocking.withConnection(ds -> {
            ds.value(String.class).set("sentinel:scoped", "s");
            assertThat(ds.value(String.class).get("sentinel:scoped")).isEqualTo("s");
        });
        assertThat(blocking.value(String.class).get("sentinel:scoped")).isEqualTo("s");
    }

    @Test
    void selectsTheDatabase() {
        ValueCommands<String, String> values = blocking.value(String.class);
        try {
            values.set("sentinel:select", "db0");
            blocking.select(1);
            assertThat(values.get("sentinel:select")).isNull();
            blocking.select(0);
            assertThat(values.get("sentinel:select")).isEqualTo("db0");
        } finally {
            blocking.select(0);
        }
    }

    @Test
    void publishesAndSubscribesOnTheMaster() {
        // The subscriber's connection is resolved through the sentinels as well, so it lands on the master.
        PubSubCommands<String> pubsub = blocking.pubsub(String.class);
        List<String> received = new CopyOnWriteArrayList<>();
        RedisSubscriber subscriber = pubsub.subscribe("sentinel:channel", received::add);
        try {
            pubsub.publish("sentinel:channel", "hello");
            await().atMost(Duration.ofSeconds(5)).until(() -> received.equals(List.of("hello")));
        } finally {
            subscriber.unsubscribe();
        }
    }
}
