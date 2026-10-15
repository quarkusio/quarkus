package io.quarkus.mongodb.panache.common.reactive;

import com.mongodb.reactivestreams.client.ClientSession;

import io.vertx.core.internal.VertxBootstrap;
import io.vertx.core.spi.VertxServiceProvider;
import io.vertx.core.spi.context.storage.ContextLocal;

public class MongodbPanacheContextLocalsProvider implements VertxServiceProvider {

    static final ContextLocal<ClientSession> SESSION_LOCAL = ContextLocal.registerLocal(ClientSession.class);

    @Override
    public void init(VertxBootstrap builder) {
        // ContextLocal registration happens via the static field above.
    }
}
