package io.quarkus.redis.client.runtime;

import java.net.URI;
import java.util.Set;

import io.quarkus.redis.client.runtime.RedisConfig.RedisConfiguration;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;

public class RedisClientUtil {
    public static final String DEFAULT_CLIENT = "<default>";

    public static RedisOptions buildOptions(RedisConfiguration redisConfig) {
        RedisOptions options = new RedisOptions();
        options.setType(redisConfig.clientType);

        if (RedisClientType.STANDALONE == redisConfig.clientType) {
            if (redisConfig.hosts.isPresent() && redisConfig.hosts.get().size() > 1) {
                throw new ConfigurationException("Multiple hosts supplied for non clustered configuration");
            }
        }

        if (redisConfig.hosts.isPresent()) {
            Set<URI> hosts = redisConfig.hosts.get();
            for (URI host : hosts) {
                options.addConnectionString(host.toString());
            }

        }
        options.setMaxNestedArrays(redisConfig.maxNestedArrays);
        options.setMaxWaitingHandlers(redisConfig.maxWaitingHandlers);
        options.setMaxPoolSize(redisConfig.maxPoolSize);
        options.setMaxPoolWaiting(redisConfig.maxPoolWaiting);
        options.setPoolRecycleTimeout(Math.toIntExact(redisConfig.poolRecycleTimeout.toMillis()));

        if (redisConfig.poolCleanerInterval.isPresent()) {
            options.setPoolCleanerInterval(Math.toIntExact(redisConfig.poolCleanerInterval.get().toMillis()));
        }

        if (redisConfig.role.isPresent()) {
            options.setRole(redisConfig.role.get());
        }

        if (redisConfig.masterName.isPresent()) {
            options.setMasterName(redisConfig.masterName.get());
        }

        if (redisConfig.replicas.isPresent()) {
            options.setUseReplicas(redisConfig.replicas.get());
        }

        return options;
    }

    public static boolean isDefault(String clientName) {
        return DEFAULT_CLIENT.equals(clientName);
    }

    public static RedisConfiguration getConfiguration(RedisConfig config, String name) {
        return isDefault(name) ? config.defaultClient : config.additionalRedisClients.get(name);
    }
}
