package io.quarkus.redis.client.runtime;

import io.vertx.redis.client.RedisAPI;

public class MutinyRedisAPI extends io.vertx.mutiny.redis.client.RedisAPI {
    MutinyRedisAPI(RedisAPI delegate) {
        super(delegate);
    }

    MutinyRedisAPI() {
        super(null);
    }
}
