package io.quarkus.redis.datasource.keys;

public enum RedisValueType {

    STRING,
    LIST,
    SET,
    ZSET,
    HASH,
    STREAM,

    NONE
}
