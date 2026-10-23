package io.quarkus.redis.lettuce.deployment.sentinel;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.lettuce.deployment.RedisSentinelTestResource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * {@code topology=static} takes the configured hosts as the nodes, and only them. Unlike the Vert.x client, which
 * trusts the order (first the master), Lettuce asks each node for its role: the hosts of the default client are
 * configured in the wrong order here and the writes still reach the master. The {@code first} and {@code second}
 * clients each list a single data node: the one that is the master must not discover its replica behind the user's
 * back, so its reads stay on the master even with {@code replicas=always}.
 */
@QuarkusTestResource(RedisSentinelTestResource.class)
public class LettuceReplicationStaticTopologyTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            // the test runs inside the application: its helper must be part of the archive
            .withApplicationRoot(jar -> jar.addClass(MasterReplicaNodes.class))
            .overrideConfigKey("quarkus.redis.hosts", "${redis.replication.reversed-hosts}")
            .overrideConfigKey("quarkus.redis.client-type", "replication")
            .overrideConfigKey("quarkus.redis.topology", "static")
            .overrideConfigKey("quarkus.redis.first.hosts", "${redis.replication.master}")
            .overrideConfigKey("quarkus.redis.first.client-type", "replication")
            .overrideConfigKey("quarkus.redis.first.topology", "static")
            .overrideConfigKey("quarkus.redis.first.replicas", "always")
            .overrideConfigKey("quarkus.redis.second.hosts", "${redis.replication.replica}")
            .overrideConfigKey("quarkus.redis.second.client-type", "replication")
            .overrideConfigKey("quarkus.redis.second.topology", "static")
            .overrideConfigKey("quarkus.redis.second.replicas", "always");

    @Inject
    RedisDataSource blocking;

    @Inject
    @RedisClientName("first")
    RedisDataSource first;

    @Inject
    @RedisClientName("second")
    RedisDataSource second;

    @ConfigProperty(name = "redis.replication.hosts")
    String replicationHosts;

    @ConfigProperty(name = "redis.sentinel.hosts")
    String sentinelHosts;

    @Test
    void asksTheNodesForTheirRoles() {
        try (MasterReplicaNodes nodes = new MasterReplicaNodes(replicationHosts, sentinelHosts,
                RedisSentinelTestResource.MASTER_NAME)) {
            ValueCommands<String, String> values = blocking.value(String.class);
            values.set("replication:static", "v");
            assertThat(values.get("replication:static")).isEqualTo("v");
            assertThat(blocking.execute("CONFIG", "GET", "port").get(1).toInteger()).isEqualTo(nodes.masterPort());
        }
    }

    @Test
    void keepsASingleStaticNodeAlone() {
        try (MasterReplicaNodes nodes = new MasterReplicaNodes(replicationHosts, sentinelHosts,
                RedisSentinelTestResource.MASTER_NAME)) {
            // a failover test may have swapped the roles since the deployment started: take the client whose only
            // node is the master right now
            int master = nodes.masterPort();
            int replica = nodes.ports().stream().filter(port -> port != master).findFirst().orElseThrow();
            RedisDataSource alone = master == nodes.ports().get(0) ? first : second;

            ValueCommands<String, String> values = alone.value(String.class);
            values.set("replication:single", "v");
            long masterBefore = nodes.gets(master);
            long replicaBefore = nodes.gets(replica);
            for (int i = 0; i < 10; i++) {
                assertThat(values.get("replication:single")).isEqualTo("v");
            }
            // the replica is not a node of this client: autodiscovery would have sent the reads there
            assertThat(nodes.gets(master) - masterBefore).isEqualTo(10);
            assertThat(nodes.gets(replica) - replicaBefore).isZero();
        }
    }
}
