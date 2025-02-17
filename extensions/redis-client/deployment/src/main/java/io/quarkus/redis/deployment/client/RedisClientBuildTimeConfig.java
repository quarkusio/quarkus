package io.quarkus.redis.deployment.client;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface RedisClientBuildTimeConfig {

    /**
     * A list of files allowing to pre-load data into the Redis server.
     * The file is formatted as follows:
     * <ul>
     * <li>One instruction per line</li>
     * <li>Each instruction is a Redis command and its parameter such as {@code HSET foo field value}</li>
     * <li>Parameters can be wrapped into double-quotes if they include spaces</li>
     * <li>Parameters can be wrapped into single-quote if they include spaces</li>
     * <li>Parameters including double-quotes must be wrapped into single-quotes</li>
     * </ul>
     */
    @ConfigDocDefault("import.redis in DEV, TEST ; no-file otherwise")
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> loadScript();

    /**
     * When using {@code redisLoadScript}, indicates if the Redis database must be flushed (erased) before importing.
     */
    @WithDefault("true")
    boolean flushBeforeLoad();

    /**
     * When using {@code redisLoadScript}, indicates if the import should only happen if the database is empty (no keys).
     */
    @WithDefault("true")
    boolean loadOnlyIfEmpty();

}
