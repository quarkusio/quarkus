package io.quarkus.arc.spi;

import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;

/**
 * This class allows us to offload a non-blocking startup method to a non-blocking thread. This is implemented in the
 * Quarkus Vert.x extension.
 */
public interface NonBlockingProvider {
    /**
     * Subscribes to the supplied {@link Uni} on a Vertx duplicated context; blocks the current thread and waits for the result.
     * <p>
     * If it's necessary, the CDI request context is activated during execution of the asynchronous code.
     *
     * @param uniSupplier
     * @throws IllegalStateException If called on an event loop thread.
     */
    <T> T subscribeAndAwait(Supplier<Uni<T>> uniSupplier) throws Throwable;
}
