package io.quarkus.redis.lettuce.deployment.sentinel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.lettuce.deployment.RedisSentinelTestResource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * {@code role=replica} on a Sentinel client maps onto {@link ReadFrom#REPLICA_PREFERRED}: the reads are served by
 * the replica the sentinels report, the writes still go to the master (the Vert.x client sends them to the replica,
 * where they fail).
 */
@QuarkusTestResource(RedisSentinelTestResource.class)
public class LettuceSentinelReplicaRoleTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            // the test runs inside the application: its helper must be part of the archive
            .withApplicationRoot(jar -> jar.addClass(MasterReplicaNodes.class))
            .overrideConfigKey("quarkus.redis.hosts", "${redis.sentinel.hosts}")
            .overrideConfigKey("quarkus.redis.client-type", "sentinel")
            .overrideConfigKey("quarkus.redis.master-name", "${redis.sentinel.master-name}")
            .overrideConfigKey("quarkus.redis.role", "replica");

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
    void readsFromTheReplicaAndWritesToTheMaster() {
        nodes = new MasterReplicaNodes(replicationHosts, sentinelHosts, RedisSentinelTestResource.MASTER_NAME);
        assertThat(connection.getReadFrom()).isEqualTo(ReadFrom.REPLICA_PREFERRED);
        int master = nodes.masterPort();
        int replica = nodes.ports().stream().filter(port -> port != master).findFirst().orElseThrow();

        ValueCommands<String, String> values = blocking.value(String.class);
        values.set("sentinel:replica-role", "v");
        // replication is asynchronous
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(values.get("sentinel:replica-role")).isEqualTo("v"));

        long replicaBefore = nodes.gets(replica);
        long masterBefore = nodes.gets(master);
        for (int i = 0; i < 10; i++) {
            assertThat(values.get("sentinel:replica-role")).isEqualTo("v");
        }
        assertThat(nodes.gets(replica) - replicaBefore).isEqualTo(10);
        assertThat(nodes.gets(master) - masterBefore).isZero();
    }
}
