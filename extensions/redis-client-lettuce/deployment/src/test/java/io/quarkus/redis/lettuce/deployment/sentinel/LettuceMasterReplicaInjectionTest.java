package io.quarkus.redis.lettuce.deployment.sentinel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.quarkus.arc.InactiveBeanException;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.lettuce.deployment.RedisSentinelTestResource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * The Lettuce beans of a Sentinel client: {@link RedisClient}, {@code StatefulRedisConnection<String, String>} and
 * {@code StatefulRedisMasterReplicaConnection<String, String>} (the same connection) are active, the cluster beans
 * are inactive; and the master/replica connection bean of a standalone client is inactive, naming the type to inject.
 */
@QuarkusTestResource(RedisSentinelTestResource.class)
public class LettuceMasterReplicaInjectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${redis.sentinel.hosts}")
            .overrideConfigKey("quarkus.redis.client-type", "sentinel")
            .overrideConfigKey("quarkus.redis.master-name", "${redis.sentinel.master-name}")
            .overrideConfigKey("quarkus.redis.plain.hosts", "${redis.replication.master}");

    @Inject
    RedisClient client;

    @Inject
    StatefulRedisConnection<String, String> connection;

    @Inject
    StatefulRedisMasterReplicaConnection<String, String> masterReplicaConnection;

    @Inject
    Instance<RedisClusterClient> clusterClient;

    @Inject
    @RedisClientName("plain")
    Instance<StatefulRedisMasterReplicaConnection<String, String>> plainMasterReplicaConnection;

    @Test
    void masterReplicaBeansAreActive() {
        assertThat(client).isNotNull();
        masterReplicaConnection.sync().set("injection:sentinel", "v");
        assertThat(connection.sync().get("injection:sentinel")).isEqualTo("v");
        // both beans expose the one connection the client opened
        assertThat(connection.sync().clientId()).isEqualTo(masterReplicaConnection.sync().clientId());
    }

    @Test
    void clusterBeansAreInactive() {
        assertThatThrownBy(() -> clusterClient.get().getPartitions())
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContaining("not configured as a cluster");
    }

    @Test
    void masterReplicaBeanOfAStandaloneClientIsInactive() {
        assertThatThrownBy(() -> plainMasterReplicaConnection.get().isOpen())
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContaining("not configured as a Sentinel or replication client")
                .hasMessageContaining("StatefulRedisConnection<String, String>");
    }
}
