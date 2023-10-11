package io.quarkus.redis.sessions.runtime;

import java.time.Duration;
import java.util.function.Supplier;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Vertx;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.sstore.redis.RedisSessionStore;
import io.vertx.mutiny.redis.client.Redis;

@Recorder
public class RedisSessionsRecorder {
    private final RuntimeValue<RedisSessionsConfig> config;

    public RedisSessionsRecorder(RuntimeValue<RedisSessionsConfig> config) {
        this.config = config;
    }

    public Supplier<SessionStore> create(Supplier<Redis> client) {
        return new Supplier<SessionStore>() {
            @Override
            public SessionStore get() {
                Vertx vertx = VertxCoreRecorder.getVertx().get();
                Duration retryTimeout = config.getValue().retryTimeout;
                return RedisSessionStore.create(vertx, retryTimeout.toMillis(), client.get().getDelegate());
            }
        };
    }
}
