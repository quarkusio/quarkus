package io.quarkus.redis.lettuce.deployment.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode.NodeFlag;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.lettuce.deployment.RedisClusterTestResource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * {@code quarkus.redis.replicas=always} maps onto {@link ReadFrom#REPLICA_PREFERRED}: the reads of the data source
 * are served by the replicas, the writes by the upstream nodes.
 */
@QuarkusTestResource(RedisClusterTestResource.class)
public class LettuceClusterReplicasTest {

    private static final Pattern GET_CALLS = Pattern.compile("cmdstat_get:calls=(\\d+)");

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${redis.cluster.hosts}")
            .overrideConfigKey("quarkus.redis.client-type", "cluster")
            .overrideConfigKey("quarkus.redis.replicas", "always");

    @Inject
    RedisDataSource blocking;

    @Inject
    StatefulRedisClusterConnection<String, String> cluster;

    @Test
    void readsFromTheReplicas() {
        assertThat(cluster.getReadFrom()).isEqualTo(ReadFrom.REPLICA_PREFERRED);

        ValueCommands<String, String> values = blocking.value(String.class);
        values.set("replicas:key", "v");

        // Lettuce only reads from a replica once it has seen its replication offset advance (INFO replication,
        // refreshed with the topology every topology-cache-ttl), and reads from the upstream node until then: on a
        // cluster that has just been created, with no replication traffic yet, wait for the refresh that follows the
        // write above, i.e. for a read served by a replica.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Map<String, Long> before = getCallsPerNode();
            assertThat(values.get("replicas:key")).isEqualTo("v");
            assertThat(gets(before, getCallsPerNode(), NodeFlag.REPLICA)).isPositive();
        });

        // from then on, every read is served by a replica
        Map<String, Long> before = getCallsPerNode();
        for (int i = 0; i < 10; i++) {
            assertThat(values.get("replicas:key")).isEqualTo("v");
        }
        Map<String, Long> after = getCallsPerNode();
        assertThat(gets(before, after, NodeFlag.REPLICA)).isEqualTo(10);
        assertThat(gets(before, after, NodeFlag.UPSTREAM)).isZero();
    }

    /** The number of {@code GET}s the nodes with {@code role} served between two snapshots of {@link #getCallsPerNode}. */
    private long gets(Map<String, Long> before, Map<String, Long> after, NodeFlag role) {
        long gets = 0;
        for (RedisClusterNode node : cluster.getPartitions()) {
            if (node.is(role)) {
                gets += after.get(node.getNodeId()) - before.get(node.getNodeId());
            }
        }
        return gets;
    }

    /** The number of {@code GET}s each node has served so far. */
    private Map<String, Long> getCallsPerNode() {
        Map<String, Long> calls = new HashMap<>();
        for (RedisClusterNode node : cluster.getPartitions()) {
            String stats = cluster.getConnection(node.getNodeId()).sync().info("commandstats");
            Matcher matcher = GET_CALLS.matcher(stats);
            calls.put(node.getNodeId(), matcher.find() ? Long.parseLong(matcher.group(1)) : 0L);
        }
        return calls;
    }
}
