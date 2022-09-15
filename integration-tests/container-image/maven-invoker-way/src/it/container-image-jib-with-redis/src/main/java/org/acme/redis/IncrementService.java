package org.acme.redis;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

import jakarta.inject.Singleton;
import java.util.List;

@Singleton
class IncrementService {


    private final KeyCommands<String> keyCommands;
    private final ValueCommands<String, Integer> valueCommands;

    public IncrementService(RedisDataSource ds) {

        keyCommands = ds.key();
        valueCommands = ds.value(Integer.class);

    }

    void del(String key) {
        keyCommands.del(key);
    }

    Integer get(String key) {
        return valueCommands.get(key);
    }

    void set(String key, Integer value) {
        valueCommands.set(key, value);
    }

    void increment(String key, Integer incrementBy) {
        valueCommands.incrby(key, incrementBy);
    }

    List<String> keys() {
        return keyCommands.keys("*");
    }
}


