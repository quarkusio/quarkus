package io.quarkus.redis.lettuce.deployment.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.ClusterPartitionParser;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode.NodeFlag;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.lettuce.deployment.RedisClusterTestResource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * The data source recovers from a topology change: after a manual failover hands the slots of an upstream node over
 * to its replica, the commands keep being routed to the right node and the client's view of the topology catches up
 * (adaptive refresh on the {@code MOVED} redirects, periodic refresh every {@code topology-cache-ttl}).
 */
@QuarkusTestResource(RedisClusterTestResource.class)
public class LettuceClusterTopologyChangeTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${redis.cluster.hosts}")
            .overrideConfigKey("quarkus.redis.client-type", "cluster");

    @Inject
    RedisDataSource blocking;

    @Inject
    RedisClusterClient client;

    @Inject
    StatefulRedisClusterConnection<String, String> cluster;

    @Test
    void recoversAfterAFailover() {
        ValueCommands<String, String> values = blocking.value(String.class);
        String key = "failover:key";
        values.set(key, "before");

        int slot = SlotHash.getSlot(key);
        RedisClusterNode upstream = client.getPartitions().getPartitionBySlot(slot);
        RedisClusterNode replica = replicaOf(upstream);

        // the replica takes over the slots of its upstream node, which becomes its replica
        assertThat(cluster.getConnection(replica.getNodeId()).sync().clusterFailover(false)).isEqualTo("OK");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            values.set(key, "after");
            assertThat(values.get(key)).isEqualTo("after");
            RedisClusterNode owner = client.getPartitions().getPartitionBySlot(slot);
            assertThat(owner.getNodeId()).isEqualTo(replica.getNodeId());
            assertThat(owner.is(NodeFlag.UPSTREAM)).isTrue();
        });

        // keys over all the slots are still reachable
        for (int i = 0; i < 50; i++) {
            values.set("failover:" + i, Integer.toString(i));
        }
        for (int i = 0; i < 50; i++) {
            assertThat(values.get("failover:" + i)).isEqualTo(Integer.toString(i));
        }
    }

    /**
     * The cluster is shared with the other test classes of the module: leave it only once it has settled after the
     * failover, with three upstream nodes owning slots and three replicas linked to them, and once every node agrees
     * on that (the gossip about the demoted node takes a moment to reach all of them, and a client created by a later
     * test may pick its first view from any node), so that no other test observes the reconfiguration in progress.
     */
    @AfterEach
    void awaitSettledCluster() {
        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            client.refreshPartitions();
            assertSettled(client.getPartitions());
            for (RedisClusterNode node : client.getPartitions()) {
                RedisCommands<String, String> commands = cluster.getConnection(node.getNodeId()).sync();
                assertSettled(ClusterPartitionParser.parse(commands.clusterNodes()));
                if (node.is(NodeFlag.REPLICA)) {
                    assertThat(commands.info("replication")).contains("master_link_status:up");
                }
            }
        });
    }

    private static void assertSettled(Partitions partitions) {
        assertThat(partitions).hasSize(RedisClusterTestResource.NODES);
        assertThat(partitions).noneMatch(node -> node.is(NodeFlag.FAIL) || node.is(NodeFlag.EVENTUAL_FAIL));
        assertThat(partitions).filteredOn(node -> node.is(NodeFlag.UPSTREAM))
                .hasSize(RedisClusterTestResource.NODES / 2)
                .allSatisfy(node -> assertThat(node.getSlots()).isNotEmpty());
        assertThat(partitions).filteredOn(node -> node.is(NodeFlag.REPLICA))
                .hasSize(RedisClusterTestResource.NODES / 2)
                .allSatisfy(node -> assertThat(partitions.getPartitionByNodeId(node.getSlaveOf())).isNotNull());
    }

    private RedisClusterNode replicaOf(RedisClusterNode upstream) {
        for (RedisClusterNode node : client.getPartitions()) {
            if (upstream.getNodeId().equals(node.getSlaveOf())) {
                return node;
            }
        }
        throw new AssertionError("No replica of " + upstream.getNodeId());
    }
}
