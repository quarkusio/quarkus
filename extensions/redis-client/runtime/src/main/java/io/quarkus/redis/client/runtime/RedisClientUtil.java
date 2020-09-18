package io.quarkus.redis.client.runtime;

import java.net.URI;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.redis.client.runtime.RedisConfig.RedisConfiguration;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;

public class RedisClientUtil {
    public static final String DEFAULT_CLIENT = "<default>";
    private static final Logger LOGGER = Logger.getLogger(RedisClientUtil.class);

    public static RedisOptions buildOptions(RedisConfiguration redisConfig, String clientName) {
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
                options.addConnectionString(buildConnectionString(redisConfig, host, clientName));
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

        if (redisConfig.slaves.isPresent()) {
            options.setUseSlave(redisConfig.slaves.get());
        }

        return options;
    }

    public static boolean isDefault(String clientName) {
        return DEFAULT_CLIENT.equals(clientName);
    }

    public static RedisConfiguration getConfiguration(RedisConfig config, String name) {
        return isDefault(name) ? config.defaultClient : config.additionalRedisClients.get(name);
    }

    /**
     * @deprecated It should be removed in the 1.10 release.
     *             This method was only added to support minimal backward compatibility.
     *             <p>
     *             Follows up https://github.com/quarkusio/quarkus/pull/11908#issuecomment-694794724
     */
    private static String buildConnectionString(RedisConfiguration config, URI host, String clientName) {
        final String address = host.toString();
        if (address.contains("://")) {
            return address;
        }

        LOGGER.warnf(
                "The configuration property quarkus.redis%s.hosts is using the deprecated way of setting up the Redis connection. "
                        + "Visit https://quarkus.io/guides/redis#quarkus-redis-client_quarkus.redis.hosts configuration reference section for more info.",
                isDefault(clientName) ? "" : "." + clientName);

        boolean ssl = false;
        if (config.ssl.isPresent()) {
            ssl = config.ssl.get();
            logDeprecationWarning(clientName, "ssl");
        }

        final StringBuilder builder = ssl ? new StringBuilder("rediss://") : new StringBuilder("redis://");
        if (config.password.isPresent()) {
            builder.append(config.password.get());
            builder.append('@');
            logDeprecationWarning(clientName, "password");
        }

        builder.append(host.getHost());
        builder.append(':');
        builder.append(host.getPort());
        builder.append('/');

        if (config.database.isPresent()) {
            builder.append(config.database.getAsInt());
            logDeprecationWarning(clientName, "database");
        }

        return builder.toString();
    }

    /**
     * @deprecated It should be removed in the 1.10 release.
     *             This method was only added to support minimal backward compatibility.
     *             <p>
     *             Follows up https://github.com/quarkusio/quarkus/pull/11908#issuecomment-694794724
     */
    private static void logDeprecationWarning(String clientName, String propertyName) {
        LOGGER.warnf("The configuration property quarkus.redis%s.%s is deprecated. It will be removed in the future release. "
                + "Visit https://quarkus.io/guides/redis#quarkus-redis-client_quarkus.redis.hosts configuration reference section for more info.",
                isDefault(clientName) ? "" : "." + clientName, propertyName);
    }
}
