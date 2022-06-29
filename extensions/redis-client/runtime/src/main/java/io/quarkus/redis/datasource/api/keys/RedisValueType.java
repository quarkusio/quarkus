package io.quarkus.redis.datasource.api.keys;

public enum RedisValueType {

    STRING,
    LIST,
    SET,
    ZSET,
    HASH,
    STREAM,

    NONE
}
