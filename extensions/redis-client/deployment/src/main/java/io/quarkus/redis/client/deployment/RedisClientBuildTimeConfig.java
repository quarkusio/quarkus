package io.quarkus.redis.client.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ConfigGroup
public class RedisClientBuildTimeConfig {

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
    @ConfigItem(defaultValueDocumentation = "import.redis in DEV, TEST ; no-file otherwise")
    @ConvertWith(TrimmedStringConverter.class)
    public Optional<List<String>> loadScript;

    /**
     * When using {@code redisLoadScript}, indicates if the Redis database must be flushed (erased) before importing.
     */
    @ConfigItem(defaultValue = "true")
    public boolean flushBeforeLoad;

    /**
     * When using {@code redisLoadScript}, indicates if the import should only happen if the database is empty (no keys).
     */
    @ConfigItem(defaultValue = "true")
    public boolean loadOnlyIfEmpty;

}
