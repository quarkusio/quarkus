package io.quarkus.redis.lettuce.deployment.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode.NodeFlag;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyScanArgs;
import io.quarkus.redis.datasource.keys.KeyScanCursor;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands.RedisSubscriber;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.lettuce.deployment.RedisClusterTestResource;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * The Lettuce data sources on a Redis cluster ({@code client-type=cluster}): the topology is discovered from the
 * configured seed nodes and every command is routed to the node owning its key, so keys spread over the nodes and
 * {@code SCAN} visits all of them. Multi-key commands and transactions surface the same class of error as the Vert.x
 * backend: a {@code CROSSSLOT} error from the server for keys in different slots, a failure for
 * {@code withTransaction}, which neither backend supports on a cluster by default.
 */
@QuarkusTestResource(RedisClusterTestResource.class)
public class LettuceClusterDataSourceTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${redis.cluster.hosts}")
            .overrideConfigKey("quarkus.redis.client-type", "cluster");

    @Inject
    RedisDataSource blocking;

    @Inject
    ReactiveRedisDataSource reactive;

    /** The connection bean of the same client, used to look at the individual nodes. */
    @Inject
    StatefulRedisClusterConnection<String, String> cluster;

    @BeforeEach
    void flush() {
        // runs on every upstream node
        blocking.flushall();
    }

    @Test
    void connectsToTheCluster() {
        assertThat(blocking.execute("PING").toString()).isEqualTo("PONG");
        assertThat(reactive.execute("PING").await().atMost(Duration.ofSeconds(5)).toString()).isEqualTo("PONG");
        assertThat(cluster.getPartitions()).hasSize(RedisClusterTestResource.NODES);
        assertThat(upstreamNodes()).hasSize(RedisClusterTestResource.NODES / 2);
    }

    @Test
    void spreadsTheKeysOverTheNodes() {
        ValueCommands<String, String> values = blocking.value(String.class);
        for (int i = 0; i < 50; i++) {
            values.set("spread:" + i, Integer.toString(i));
        }
        for (int i = 0; i < 50; i++) {
            assertThat(values.get("spread:" + i)).isEqualTo(Integer.toString(i));
        }
        // 50 keys land on several slots, and every upstream node owns some of them
        List<Long> keysPerNode = new ArrayList<>();
        for (RedisClusterNode node : upstreamNodes()) {
            keysPerNode.add(cluster.getConnection(node.getNodeId()).sync().dbsize());
        }
        assertThat(keysPerNode).allSatisfy(count -> assertThat(count).isPositive());
        assertThat(keysPerNode.stream().mapToLong(Long::longValue).sum()).isEqualTo(50);
    }

    @Test
    void scansAllTheNodes() {
        ValueCommands<String, String> values = blocking.value(String.class);
        Set<String> expected = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            values.set("scan:" + i, "v");
            expected.add("scan:" + i);
        }

        KeyScanCursor<String> cursor = blocking.key(String.class).scan(new KeyScanArgs().match("scan:*").count(5));
        Set<String> scanned = new HashSet<>();
        while (cursor.hasNext()) {
            scanned.addAll(cursor.next());
        }
        assertThat(scanned).isEqualTo(expected);

        List<String> reactivelyScanned = reactive.key(String.class).scan(new KeyScanArgs().match("scan:*")).toMulti()
                .collect().asList().await().atMost(Duration.ofSeconds(5));
        assertThat(reactivelyScanned).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void routesMultiKeyCommandsOfASlotAndRejectsCrossSlotOnes() {
        var sets = blocking.set(String.class);
        sets.sadd("{inter}:a", "x", "y", "z");
        sets.sadd("{inter}:b", "y", "z");
        // keys sharing a hash tag are in the same slot
        assertThat(sets.sinter("{inter}:a", "{inter}:b")).containsExactlyInAnyOrder("y", "z");

        String a = "cross:a";
        String b = otherSlotThan(a);
        sets.sadd(a, "x");
        sets.sadd(b, "x");
        assertThatThrownBy(() -> sets.sinter(a, b))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("CROSSSLOT");
        assertThatThrownBy(() -> reactive.set(String.class).sinter(a, b).await().atMost(Duration.ofSeconds(5)))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("CROSSSLOT");

        // the commands Lettuce (like the Vert.x client) splits per slot work across slots
        ValueCommands<String, String> values = blocking.value(String.class);
        values.set(a, "1");
        values.set(b, "2");
        Map<String, String> got = values.mget(a, b);
        assertThat(got).containsEntry(a, "1").containsEntry(b, "2");
        assertThat(blocking.key(String.class).del(a, b)).isEqualTo(2);
    }

    @Test
    void routesRawCommandsByKey() {
        // one key per upstream node, so that whatever the default node is, some keys live elsewhere
        Map<String, RedisClusterNode> keys = new java.util.LinkedHashMap<>();
        for (int i = 0; keys.size() < upstreamNodes().size(); i++) {
            String key = "raw:" + i;
            RedisClusterNode owner = cluster.getPartitions().getPartitionBySlot(SlotHash.getSlot(key));
            if (keys.values().stream().noneMatch(node -> node.getNodeId().equals(owner.getNodeId()))) {
                keys.put(key, owner);
            }
        }
        for (String key : keys.keySet()) {
            blocking.execute("SET", key, "v");
        }
        Map<String, long[]> before = getStats();

        for (int i = 0; i < 5; i++) {
            for (String key : keys.keySet()) {
                assertThat(blocking.execute("GET", key).toString()).isEqualTo("v");
            }
        }

        // every GET was served by the node owning its key: no node answered one with a MOVED redirect
        Map<String, long[]> after = getStats();
        for (RedisClusterNode node : upstreamNodes()) {
            long served = after.get(node.getNodeId())[0] - before.get(node.getNodeId())[0];
            long rejected = after.get(node.getNodeId())[1] - before.get(node.getNodeId())[1];
            assertThat(rejected).as("GETs answered with MOVED by %s", node.getNodeId()).isZero();
            assertThat(served).as("GETs served by %s", node.getNodeId()).isEqualTo(5);
        }
    }

    /** Per upstream node: the {@code GET}s it served and the ones it rejected (with a {@code MOVED} redirect). */
    private Map<String, long[]> getStats() {
        Map<String, long[]> stats = new java.util.HashMap<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern
                .compile("cmdstat_get:calls=(\\d+),[^\\n]*rejected_calls=(\\d+)");
        for (RedisClusterNode node : upstreamNodes()) {
            java.util.regex.Matcher matcher = pattern
                    .matcher(cluster.getConnection(node.getNodeId()).sync().info("commandstats"));
            stats.put(node.getNodeId(), matcher.find()
                    ? new long[] { Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2)) }
                    : new long[] { 0, 0 });
        }
        return stats;
    }

    @Test
    void rejectsTransactionsAtOnce() {
        ValueCommands<String, String> values = blocking.value(String.class);
        values.set("tx:key", "before");

        assertThatThrownBy(() -> blocking.withTransaction(tx -> tx.value(String.class).set("tx:key", "after")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage(LettuceConnection.TRANSACTIONS_NOT_SUPPORTED_ON_CLUSTER);
        assertThatThrownBy(() -> blocking.withTransaction(tx -> tx.value(String.class).set("tx:key", "after"), "tx:key"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("cluster");
        assertThatThrownBy(() -> blocking.withTransaction(ds -> ds.value(String.class).get("tx:key"),
                (current, tx) -> tx.value(String.class).set("tx:key", current + "!"), "tx:key"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("cluster");
        assertThatThrownBy(() -> reactive.withTransaction(tx -> tx.value(String.class).set("tx:key", "after"))
                .await().atMost(Duration.ofSeconds(5)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage(LettuceConnection.TRANSACTIONS_NOT_SUPPORTED_ON_CLUSTER);
        // also on a pinned data source
        blocking.withConnection(ds -> assertThatThrownBy(
                () -> ds.withTransaction(tx -> tx.value(String.class).set("tx:key", "after")))
                .isInstanceOf(UnsupportedOperationException.class));

        // nothing was sent: the transaction failed before any connection was borrowed
        assertThat(values.get("tx:key")).isEqualTo("before");
    }

    @Test
    void rejectsSelectWithoutHarmingThePooledConnections() {
        // a cluster has a single database: the server rejects SELECT, as with the Vert.x client
        assertThatThrownBy(() -> blocking.select(1))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("SELECT");
        assertThatThrownBy(() -> blocking.execute("SELECT", "1"))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("SELECT");

        // a rejected SELECT on a pooled connection leaves nothing to reset before its next use
        blocking.withConnection(ds -> {
            assertThatThrownBy(() -> ds.select(1)).isInstanceOf(RedisCommandExecutionException.class);
            ds.value(String.class).set("select:key", "v");
        });
        blocking.withConnection(ds -> assertThat(ds.value(String.class).get("select:key")).isEqualTo("v"));
        assertThat(blocking.value(String.class).get("select:key")).isEqualTo("v");
    }

    @Test
    void runsBlockingCommandsAndScopedConnections() {
        var lists = blocking.list(String.class);
        lists.rpush("blocking:list", "a");
        KeyValue<String, String> popped = lists.blpop(Duration.ofSeconds(1), "blocking:list");
        assertThat(popped).isEqualTo(KeyValue.of("blocking:list", "a"));
        assertThat(lists.blpop(Duration.ofMillis(100), "blocking:list")).isNull();

        blocking.withConnection(ds -> {
            ds.value(String.class).set("scoped:key", "v1");
            assertThat(ds.value(String.class).get("scoped:key")).isEqualTo("v1");
        });
        assertThat(blocking.value(String.class).get("scoped:key")).isEqualTo("v1");

        reactive.withConnection(ds -> ds.value(String.class).set("scoped:reactive", "v2"))
                .await().atMost(Duration.ofSeconds(5));
        assertThat(blocking.value(String.class).get("scoped:reactive")).isEqualTo("v2");
    }

    @Test
    void publishesAndSubscribesAcrossTheCluster() {
        // Pub/Sub is not tied to a slot: a message published to any node reaches a subscriber connected to any
        // other, and the subscriber's own connection is to a node Lettuce picks.
        PubSubCommands<String> pubsub = blocking.pubsub(String.class);
        List<String> received = new CopyOnWriteArrayList<>();
        RedisSubscriber subscriber = pubsub.subscribe("cluster:channel", received::add);
        try {
            pubsub.publish("cluster:channel", "hello");
            reactive.pubsub(String.class).publish("cluster:channel", "world").await().atMost(Duration.ofSeconds(5));
            await().atMost(Duration.ofSeconds(5)).until(() -> received.size() == 2);
            assertThat(received).containsExactly("hello", "world");
        } finally {
            subscriber.unsubscribe();
        }
    }

    private List<RedisClusterNode> upstreamNodes() {
        List<RedisClusterNode> upstream = new ArrayList<>();
        for (RedisClusterNode node : cluster.getPartitions()) {
            if (node.is(NodeFlag.UPSTREAM)) {
                upstream.add(node);
            }
        }
        return upstream;
    }

    /** A key whose slot is owned by another node than the slot of {@code key}. */
    private String otherSlotThan(String key) {
        RedisClusterNode owner = cluster.getPartitions().getPartitionBySlot(SlotHash.getSlot(key));
        for (int i = 0;; i++) {
            String candidate = "cross:" + i;
            RedisClusterNode candidateOwner = cluster.getPartitions().getPartitionBySlot(SlotHash.getSlot(candidate));
            if (!candidateOwner.getNodeId().equals(owner.getNodeId())) {
                return candidate;
            }
        }
    }
}
