package io.quarkus.vertx.core.runtime;

import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.internal.VertxBootstrap;
import io.vertx.core.spi.VertxServiceProvider;

// Temporary - we need to implement this SPI in SmallRye commons and move the local declaration in the provider
// implementation, as done for MDC.
public class ContextLocalsVertxServiceProvider implements VertxServiceProvider {
    @Override
    public void init(VertxBootstrap builder) {
        // Just touch the class.
        VertxContext.isOnDuplicatedContext();
    }
}
