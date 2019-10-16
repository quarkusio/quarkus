package io.quarkus.redis.it;

import java.util.List;
import java.util.Map;

import io.lettuce.core.Value;
import io.lettuce.core.dynamic.Commands;

public interface SyncCommands extends Commands {
    default String defaultGet(String key) {
        return this.get(key);
    }

    String get(String key);

    String set(String key, String value);

    String set(String key, byte[] value);

    void hset(String key, String key1, String value);

    Map<String, String> hgetall(String key);

    void lpush(String key, String value);

    String lpop(String key);

    void geoadd(String key, Double v, Double v1, String v2);

    List<Value<String>> geohash(String key, String... objects);
}
