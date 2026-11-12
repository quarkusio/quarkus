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

import io.lettuce.core.ReadFrom;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands.RedisSubscriber;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.lettuce.deployment.RedisSentinelTestResource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * The Lettuce data sources on a replicated master ({@code client-type=replication}, default {@code topology=discover}):
 * the master and its replica are discovered from the first host, the writes go to the master and, with
 * {@code replicas=always}, the reads to the replica. Transactions work as on a standalone server.
 */
@QuarkusTestResource(RedisSentinelTestResource.class)
public class LettuceReplicationDataSourceTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            // the test runs inside the application: its helper must be part of the archive
            .withApplicationRoot(jar -> jar.addClass(MasterReplicaNodes.class))
            .overrideConfigKey("quarkus.redis.hosts", "${redis.replication.hosts}")
            .overrideConfigKey("quarkus.redis.client-type", "replication")
            .overrideConfigKey("quarkus.redis.replicas", "always");

    private static MasterReplicaNodes nodes;

    @Inject
    RedisDataSource blocking;

    @Inject
    StatefulRedisMasterReplicaConnection<String, String> connection;

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

    @Test
    void writesToTheMasterAndReadsFromTheReplica() {
        nodes = new MasterReplicaNodes(replicationHosts, sentinelHosts, RedisSentinelTestResource.MASTER_NAME);
        assertThat(connection.getReadFrom()).isEqualTo(ReadFrom.REPLICA_PREFERRED);
        int master = nodes.masterPort();
        int replica = nodes.ports().stream().filter(port -> port != master).findFirst().orElseThrow();

        ValueCommands<String, String> values = blocking.value(String.class);
        values.set("replication:key", "v");
        assertThat(blocking.execute("CONFIG", "GET", "port").get(1).toInteger()).isEqualTo(master);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(values.get("replication:key")).isEqualTo("v"));

        long replicaBefore = nodes.gets(replica);
        long masterBefore = nodes.gets(master);
        for (int i = 0; i < 10; i++) {
            assertThat(values.get("replication:key")).isEqualTo("v");
        }
        assertThat(nodes.gets(replica) - replicaBefore).isEqualTo(10);
        assertThat(nodes.gets(master) - masterBefore).isZero();
    }

    @Test
    void supportsTransactions() {
        TransactionResult result = blocking.withTransaction(tx -> {
            tx.value(String.class).set("replication:tx", "a");
            tx.value(String.class).get("replication:tx");
        });
        assertThat(result.discarded()).isFalse();
        assertThat((String) result.get(1)).isEqualTo("a");
    }

    @Test
    void publishesAndSubscribesThroughReplication() {
        // The subscriber connects to the first configured host, whichever role it has; a message published to the
        // master reaches it either way, as Redis propagates PUBLISH to the replicas.
        PubSubCommands<String> pubsub = blocking.pubsub(String.class);
        List<String> received = new CopyOnWriteArrayList<>();
        RedisSubscriber subscriber = pubsub.subscribe("replication:channel", received::add);
        try {
            pubsub.publish("replication:channel", "hello");
            await().atMost(Duration.ofSeconds(5)).until(() -> received.equals(List.of("hello")));
        } finally {
            subscriber.unsubscribe();
        }
    }
}
