package io.quarkus.redis.lettuce.deployment.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode.NodeFlag;
import io.quarkus.arc.InactiveBeanException;
import io.quarkus.redis.lettuce.deployment.RedisClusterTestResource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * The Lettuce beans of a cluster client: {@link RedisClusterClient} and
 * {@code StatefulRedisClusterConnection<String, String>} are active, the standalone {@link RedisClient} and
 * {@code StatefulRedisConnection<String, String>} beans are inactive and say which types to inject instead.
 */
@QuarkusTestResource(RedisClusterTestResource.class)
public class LettuceClusterInjectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${redis.cluster.hosts}")
            .overrideConfigKey("quarkus.redis.client-type", "cluster");

    @Inject
    RedisClusterClient client;

    @Inject
    StatefulRedisClusterConnection<String, String> connection;

    @Inject
    Instance<RedisClient> redisClient;

    @Inject
    Instance<StatefulRedisConnection<String, String>> standaloneConnection;

    @Test
    void clusterBeansAreActive() {
        assertThat(client.getPartitions()).hasSize(RedisClusterTestResource.NODES);
        assertThat(client.getPartitions()).filteredOn(node -> node.is(NodeFlag.UPSTREAM)).hasSize(3);

        connection.sync().set("injection:key", "v");
        assertThat(connection.sync().get("injection:key")).isEqualTo("v");
        assertThat(connection.isOpen()).isTrue();
    }

    @Test
    void standaloneBeansAreInactive() {
        // the beans are application scoped: the activation is checked when the client proxy is first used
        assertThatThrownBy(() -> redisClient.get().getOptions())
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContaining("configured as a cluster")
                .hasMessageContaining("RedisClusterClient");
        assertThatThrownBy(() -> standaloneConnection.get().isOpen())
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContaining("StatefulRedisClusterConnection");
    }
}
