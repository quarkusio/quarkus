package io.quarkus.hibernate.reactive.runtime;

import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.internal.VertxBootstrap;
import io.vertx.core.spi.VertxServiceProvider;
import io.vertx.core.spi.context.storage.ContextLocal;

public class HibernateReactiveVertxServiceProvider<T> implements VertxServiceProvider {

    @SuppressWarnings("unchecked")
    static final ContextLocal<ConcurrentHashMap<Object, Object>> SESSIONS_LOCAL = (ContextLocal<ConcurrentHashMap<Object, Object>>) (ContextLocal<?>) ContextLocal
            .registerLocal(ConcurrentHashMap.class);

    @SuppressWarnings("unchecked")
    static final ContextLocal<ConcurrentHashMap<Object, Object>> STATELESS_SESSIONS_LOCAL = (ContextLocal<ConcurrentHashMap<Object, Object>>) (ContextLocal<?>) ContextLocal
            .registerLocal(ConcurrentHashMap.class);

    @Override
    public void init(VertxBootstrap builder) {
        // ContextLocal registration happens via the static fields above.
    }
}
