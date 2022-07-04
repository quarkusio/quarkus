package io.quarkus.redis.runtime.datasource;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class AbstractRedisCommands {

    protected final RedisCommandExecutor redis;
    protected final Marshaller marshaller;

    public AbstractRedisCommands(RedisCommandExecutor redis, Marshaller marshaller) {
        this.redis = redis;
        this.marshaller = marshaller;
    }

    public Uni<Response> execute(RedisCommand cmd) {
        return redis.execute(cmd.toRequest());
    }

}
