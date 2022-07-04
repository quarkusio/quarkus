package org.acme.redis;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.string.StringCommands;

import javax.inject.Singleton;
import java.util.List;

@Singleton
class IncrementService {


    private final KeyCommands<String> keyCommands;
    private final StringCommands<String, Integer> stringCommands;

    public IncrementService(RedisDataSource ds) {

        keyCommands = ds.key();
        stringCommands = ds.string(Integer.class);

    }

    void del(String key) {
        keyCommands.del(key);
    }

    Integer get(String key) {
        return stringCommands.get(key);
    }

    void set(String key, Integer value) {
        stringCommands.set(key, value);
        ;
    }

    void increment(String key, Integer incrementBy) {
        stringCommands.incrby(key, incrementBy);
    }

    List<String> keys() {
        return keyCommands.keys("*");
    }
}


