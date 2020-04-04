package io.quarkus.redis.client.runtime;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.vertx.redis.client.RedisClientType;

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
     * The maximum delay to wait before a blocking command to redis server times out
     */
    @ConfigItem(defaultValue = "10s")
    public Optional<Duration> timeout;

    /**
     * Enables or disables the SSL on connect.
     */
    @ConfigItem
    public boolean ssl;

    /**
     * The redis client type
     */
    @ConfigItem(defaultValue = "standalone")
    public RedisClientType clientType;
}
