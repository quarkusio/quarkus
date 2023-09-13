package io.quarkus.redis.runtime.datasource;

import java.util.Set;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.ResponseType;

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

    static boolean isMap(Response response) {
        try {
            return response != null && response.type() == ResponseType.MULTI && notEmptyOrNull(response.getKeys());
        } catch (Exception ignored) {
            // Not a map, but a plain multi
            return false;
        }
    }

    private static boolean notEmptyOrNull(Set<String> keys) {
        return keys != null && !keys.isEmpty();
    }

}
