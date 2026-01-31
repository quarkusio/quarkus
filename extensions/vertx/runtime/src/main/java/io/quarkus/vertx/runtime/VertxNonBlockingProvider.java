package io.quarkus.vertx.runtime;

import java.util.function.Supplier;

import io.quarkus.arc.spi.NonBlockingProvider;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

/**
 * This is used by the Quarkus Arc extension to delegate non-blocking @Startup methods
 * to VertxContextSupport without depending on this extension
 */
public class VertxNonBlockingProvider implements NonBlockingProvider {
    @Override
    public <T> T subscribeAndAwait(Supplier<Uni<T>> uniSupplier) throws Throwable {
        return VertxContextSupport.subscribeAndAwait(uniSupplier);
    }
}
