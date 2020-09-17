package io.quarkus.redis.client.runtime;

import io.vertx.redis.client.Redis;

public class MutinyRedis extends io.vertx.mutiny.redis.client.Redis {
    MutinyRedis(Redis delegate) {
        super(delegate);
    }

    MutinyRedis() {
        super(null);
    }
}
