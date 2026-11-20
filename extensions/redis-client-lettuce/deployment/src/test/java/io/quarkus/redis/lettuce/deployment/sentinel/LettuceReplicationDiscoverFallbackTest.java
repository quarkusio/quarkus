package io.quarkus.redis.lettuce.deployment.sentinel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.lettuce.deployment.RedisSentinelTestResource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * With {@code topology=discover} the hosts are tried in order until one answers, as the Vert.x client does: the
 * first host here is a port nothing listens on, the topology is discovered from the second one, and the client works
 * as if the first host had not been there, with {@code replicas=always} sending the reads to the replica that the
 * second host reported. Both the blocking connection of the data source and the asynchronously opened pooled
 * connections ({@code withConnection}) take the fallback.
 */
@QuarkusTestResource(RedisSentinelTestResource.class)
public class LettuceReplicationDiscoverFallbackTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            // the test runs inside the application: its helper must be part of the archive
            .withApplicationRoot(jar -> jar.addClass(MasterReplicaNodes.class))
            // TCP port 1 (tcpmux) is privileged and unused: the connection is refused at once
            .overrideConfigKey("quarkus.redis.hosts", "redis://127.0.0.1:1,${redis.replication.master}")
            .overrideConfigKey("quarkus.redis.client-type", "replication")
            .overrideConfigKey("quarkus.redis.replicas", "always");

    @Inject
    RedisDataSource blocking;

    @ConfigProperty(name = "redis.replication.hosts")
    String replicationHosts;

    @ConfigProperty(name = "redis.sentinel.hosts")
    String sentinelHosts;

    @Test
    void discoversTheTopologyFromTheFirstHostThatAnswers() {
        try (MasterReplicaNodes nodes = new MasterReplicaNodes(replicationHosts, sentinelHosts,
                RedisSentinelTestResource.MASTER_NAME)) {
            int master = nodes.masterPort();
            int replica = nodes.ports().stream().filter(port -> port != master).findFirst().orElseThrow();

            ValueCommands<String, String> values = blocking.value(String.class);
            values.set("replication:fallback", "v");
            assertThat(blocking.execute("CONFIG", "GET", "port").get(1).toInteger()).isEqualTo(master);
            // replication is asynchronous
            await().atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> assertThat(values.get("replication:fallback")).isEqualTo("v"));

            // the replica was reported by the second host: the first one answered nothing
            long replicaBefore = nodes.gets(replica);
            for (int i = 0; i < 10; i++) {
                assertThat(values.get("replication:fallback")).isEqualTo("v");
            }
            assertThat(nodes.gets(replica) - replicaBefore).isEqualTo(10);
        }
    }

    @Test
    void opensPooledConnectionsThroughTheFallbackToo() {
        blocking.withConnection(connection -> {
            connection.value(String.class).set("replication:fallback-pooled", "v");
            assertThat(connection.value(String.class).get("replication:fallback-pooled")).isEqualTo("v");
        });
    }
}
