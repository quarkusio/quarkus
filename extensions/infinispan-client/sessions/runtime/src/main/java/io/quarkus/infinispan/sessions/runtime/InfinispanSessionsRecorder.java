package io.quarkus.infinispan.sessions.runtime;

import java.time.Duration;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.RemoteCacheManager;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.sstore.infinispan.InfinispanSessionStore;

@Recorder
public class InfinispanSessionsRecorder {
    private final RuntimeValue<InfinispanSessionsConfig> config;

    public InfinispanSessionsRecorder(RuntimeValue<InfinispanSessionsConfig> config) {
        this.config = config;
    }

    public Supplier<SessionStore> create(RuntimeValue<RemoteCacheManager> client) {
        return new Supplier<SessionStore>() {
            @Override
            public SessionStore get() {
                Vertx vertx = VertxCoreRecorder.getVertx().get();
                String cacheName = config.getValue().cacheName;
                Duration retryTimeout = config.getValue().retryTimeout;
                JsonObject options = new JsonObject()
                        .put("cacheName", cacheName)
                        .put("retryTimeout", retryTimeout.toMillis());
                return InfinispanSessionStore.create(vertx, options, client.getValue());
            }
        };
    }
}
