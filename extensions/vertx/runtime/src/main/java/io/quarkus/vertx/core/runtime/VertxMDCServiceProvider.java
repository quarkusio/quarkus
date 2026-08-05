package io.quarkus.vertx.core.runtime;

import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.internal.VertxBootstrap;
import io.vertx.core.spi.VertxServiceProvider;
import io.vertx.core.spi.context.storage.ContextLocal;

public class VertxMDCServiceProvider implements VertxServiceProvider {

    @SuppressWarnings("unchecked")
    static final ContextLocal<ConcurrentHashMap<String, Object>> MDC_LOCAL = (ContextLocal<ConcurrentHashMap<String, Object>>) (ContextLocal<?>) ContextLocal
            .registerLocal(ConcurrentHashMap.class, ConcurrentHashMap::new);

    @Override
    public void init(VertxBootstrap builder) {
        // ContextLocal registration happens via the static field above.
    }
}
