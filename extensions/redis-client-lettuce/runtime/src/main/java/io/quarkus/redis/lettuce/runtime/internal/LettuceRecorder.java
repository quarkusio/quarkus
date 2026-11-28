package io.quarkus.redis.lettuce.runtime.internal;

import static io.quarkus.redis.runtime.client.config.RedisConfig.HOSTS;
import static io.quarkus.redis.runtime.client.config.RedisConfig.HOSTS_PROVIDER_NAME;
import static io.quarkus.redis.runtime.client.config.RedisConfig.getPropertyName;

import java.util.function.Supplier;

import io.quarkus.arc.ActiveResult;
import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.redis.client.RedisClientType;

/**
 * Recorder of the Lettuce Redis beans.
 * <p>
 * Intentionally minimal: it only produces the {@link ActiveResult} suppliers of the synthetic beans' activation
 * checks, which have no service equivalent. The clients, connections, pools and data sources are created and shut
 * down by the services {@code LettuceProcessor} registers (see {@link LettuceClientSetup}).
 */
@Recorder
public class LettuceRecorder {

    private final RuntimeValue<RedisConfig> runtimeConfig;

    public LettuceRecorder(RuntimeValue<RedisConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    private RedisClientConfig clientConfig(String name) {
        return runtimeConfig.getValue().clients().get(name);
    }

    private static boolean isCluster(RedisClientConfig config) {
        return config.clientType() == RedisClientType.CLUSTER;
    }

    private static boolean isMasterReplica(RedisClientConfig config) {
        return config.clientType() == RedisClientType.SENTINEL || config.clientType() == RedisClientType.REPLICATION;
    }

    /**
     * The activation check of the data source beans of a client, and the first check of the Lettuce beans: the client
     * is active and has {@code hosts} or a {@code hosts-provider-name} (see {@link LettuceClientSetup#isActive}, the
     * condition the services creating the client apply).
     */
    public Supplier<ActiveResult> checkActive(final String name) {
        return () -> {
            RedisClientConfig redisClientConfig = clientConfig(name);
            if (!redisClientConfig.active()) {
                return ActiveResult.inactive(String.format(
                        """
                                Lettuce Redis Client '%s' was deactivated through configuration properties. \
                                To activate the Redis Client, set configuration property '%s' to 'true' and configure the Redis Client '%s'. \
                                Refer to https://quarkus.io/guides/redis-reference for guidance.
                                """,
                        name, getPropertyName(name, "active"), name));
            }
            if (!LettuceClientSetup.hasHosts(redisClientConfig) && redisClientConfig.hostsProviderName().isEmpty()) {
                return ActiveResult.inactive(String.format(
                        """
                                Lettuce Redis Client '%s' was deactivated automatically because the hosts are not set. \
                                To activate the Redis Client, set the configuration property '%s', or '%s' together with \
                                a RedisHostsProvider bean of that name. \
                                Refer to https://quarkus.io/guides/redis-reference for guidance.
                                """,
                        name, getPropertyName(name, HOSTS), getPropertyName(name, HOSTS_PROVIDER_NAME)));
            }
            return ActiveResult.active();
        };
    }

    /**
     * The activation check of the {@code RedisClient} and {@code StatefulRedisConnection} beans: those of an active
     * client that is not configured as a cluster (a master/replica connection is a {@code StatefulRedisConnection}).
     */
    public Supplier<ActiveResult> checkActiveStandalone(final String name) {
        return () -> {
            ActiveResult active = checkActive(name).get();
            if (!active.value()) {
                return active;
            }
            if (isCluster(clientConfig(name))) {
                return ActiveResult.inactive(String.format(
                        """
                                Lettuce Redis Client '%s' is configured as a cluster through the configuration property '%s'. \
                                Inject io.lettuce.core.cluster.RedisClusterClient and \
                                io.lettuce.core.cluster.api.StatefulRedisClusterConnection<String, String> instead of \
                                io.lettuce.core.RedisClient and io.lettuce.core.api.StatefulRedisConnection<String, String>. \
                                Refer to https://quarkus.io/guides/redis-reference for guidance.
                                """,
                        name, getPropertyName(name, "client-type")));
            }
            return ActiveResult.active();
        };
    }

    /**
     * The activation check of the {@code StatefulRedisMasterReplicaConnection} bean: that of an active client that is
     * configured as a Sentinel or replication client.
     */
    public Supplier<ActiveResult> checkActiveMasterReplica(final String name) {
        return () -> {
            ActiveResult active = checkActive(name).get();
            if (!active.value()) {
                return active;
            }
            RedisClientConfig config = clientConfig(name);
            if (!isMasterReplica(config)) {
                return ActiveResult.inactive(String.format(
                        """
                                Lettuce Redis Client '%s' is not configured as a Sentinel or replication client (its '%s' is '%s'): \
                                inject io.lettuce.core.api.StatefulRedisConnection<String, String> for a standalone client or \
                                io.lettuce.core.cluster.api.StatefulRedisClusterConnection<String, String> for a cluster instead of \
                                io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection<String, String>. \
                                Refer to https://quarkus.io/guides/redis-reference for guidance.
                                """,
                        name, getPropertyName(name, "client-type"), config.clientType().name().toLowerCase()));
            }
            return ActiveResult.active();
        };
    }

    /**
     * The activation check of the {@code RedisClusterClient} and {@code StatefulRedisClusterConnection} beans: those
     * of an active client that is configured as a cluster.
     */
    public Supplier<ActiveResult> checkActiveCluster(final String name) {
        return () -> {
            ActiveResult active = checkActive(name).get();
            if (!active.value()) {
                return active;
            }
            if (!isCluster(clientConfig(name))) {
                return ActiveResult.inactive(String.format(
                        """
                                Lettuce Redis Client '%s' is not configured as a cluster: set the configuration property '%s' to 'cluster', \
                                or inject io.lettuce.core.RedisClient and io.lettuce.core.api.StatefulRedisConnection<String, String> \
                                instead of io.lettuce.core.cluster.RedisClusterClient and \
                                io.lettuce.core.cluster.api.StatefulRedisClusterConnection<String, String>. \
                                Refer to https://quarkus.io/guides/redis-reference for guidance.
                                """,
                        name, getPropertyName(name, "client-type")));
            }
            return ActiveResult.active();
        };
    }
}
