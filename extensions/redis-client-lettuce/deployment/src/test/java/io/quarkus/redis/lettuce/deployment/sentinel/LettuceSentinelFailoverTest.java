package io.quarkus.redis.lettuce.deployment.sentinel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.lettuce.deployment.RedisSentinelTestResource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * The data source follows a Sentinel failover: after {@code SENTINEL FAILOVER} promotes the replica, the writes go
 * to the new master without the application reconnecting (Lettuce subscribes to the sentinel events), whatever
 * {@code auto-failover} says.
 */
@QuarkusTestResource(RedisSentinelTestResource.class)
public class LettuceSentinelFailoverTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            // the test runs inside the application: its helper must be part of the archive
            .withApplicationRoot(jar -> jar.addClass(MasterReplicaNodes.class))
            .overrideConfigKey("quarkus.redis.hosts", "${redis.sentinel.hosts}")
            .overrideConfigKey("quarkus.redis.client-type", "sentinel")
            .overrideConfigKey("quarkus.redis.master-name", "${redis.sentinel.master-name}");

    @Inject
    RedisDataSource blocking;

    @ConfigProperty(name = "redis.replication.hosts")
    String replicationHosts;

    @ConfigProperty(name = "redis.sentinel.hosts")
    String sentinelHosts;

    private MasterReplicaNodes nodes;

    @Test
    void followsTheNewMaster() {
        nodes = new MasterReplicaNodes(replicationHosts, sentinelHosts, RedisSentinelTestResource.MASTER_NAME);
        ValueCommands<String, String> values = blocking.value(String.class);
        values.set("sentinel:failover", "before");
        int oldMaster = nodes.masterPort();
        assertThat(blocking.execute("CONFIG", "GET", "port").get(1).toInteger()).isEqualTo(oldMaster);

        assertThat(nodes.failover()).isEqualTo("OK");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            int newMaster = nodes.masterPort();
            assertThat(newMaster).isNotEqualTo(oldMaster);
            // the data source writes to the new master, and reads back from it
            values.set("sentinel:failover", "after");
            assertThat(values.get("sentinel:failover")).isEqualTo("after");
            assertThat(blocking.execute("CONFIG", "GET", "port").get(1).toInteger()).isEqualTo(newMaster);
        });
    }

    /**
     * The deployment is shared with the other test classes of the module: leave it only once the demoted master is
     * back as a synced replica of the new one, so that no other test sees the reconfiguration in progress.
     */
    @AfterEach
    void awaitSettledDeployment() {
        if (nodes == null) {
            return;
        }
        try {
            await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
                int master = nodes.masterPort();
                assertThat(nodes.role(master)).isEqualTo("master");
                for (int port : nodes.ports()) {
                    if (port != master) {
                        assertThat(nodes.isSyncedReplica(port)).isTrue();
                    }
                }
            });
        } finally {
            nodes.close();
        }
    }
}
