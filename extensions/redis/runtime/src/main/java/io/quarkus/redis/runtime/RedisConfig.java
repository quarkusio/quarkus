package io.quarkus.redis.runtime;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class RedisConfig {

    /**
     * The redis password
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * The redis hosts
     */
    @ConfigItem(defaultValue = "localhost:6379")
    public Optional<Set<InetSocketAddress>> hosts;

    /**
     * The redis database
     */
    @ConfigItem
    public int database;

    /**
     * The redis cluster configuration
     */
    @ConfigItem
    public Optional<RedisClusterConfig> cluster;

    /**
     * Whether to enable ssl
     */
    @ConfigItem
    public boolean sslEnabled;

    @ConfigGroup
    public static class RedisClusterConfig {
        /**
         * Indicate whether to create a redis cluster connection
         */
        @ConfigItem
        public boolean enabled;
    }
}
