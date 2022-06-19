package io.quarkus.redis.datasource.api.keys;

public class RedisKeyNotFoundException extends RuntimeException {

    public RedisKeyNotFoundException(String key) {
        super("The key `" + key + "` does not exist");
    }

}
