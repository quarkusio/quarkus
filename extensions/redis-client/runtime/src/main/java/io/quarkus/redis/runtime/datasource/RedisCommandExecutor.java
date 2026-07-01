package io.quarkus.redis.runtime.datasource;

import io.smallrye.mutiny.Uni;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;

public interface RedisCommandExecutor {

    default Uni<Response> execute(RedisCommand cmd) {
        return execute(cmd.toRequest());
    }

    Uni<Response> execute(Request toRequest);

}
