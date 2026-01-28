package io.quarkus.arc.runtime;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import io.quarkus.arc.spi.NonBlockingProvider;
import io.smallrye.mutiny.Uni;

/**
 * Utility class that allows invoking non-blocking startup methods on non-blocking threads when the Quarkus Vert.x
 * extension is present (which provides us with support for this). This is used to avoid a circular dependency with
 * it. This is also checked at build time so we should never have a runtime error.
 */
public class NonBlockingSupport {
    private final static NonBlockingProvider PROVIDER;
    public static final String ERROR_MSG = "Failed to find any non-blocking provider for startup actions. Either import the quarkus-vertx module, or do not declare non-blocking startup actions.";

    static {
        // If Vert.x is not present, we will complain at run-time (should not happen since we check at build time)
        ServiceLoader<NonBlockingProvider> loader = ServiceLoader.load(NonBlockingProvider.class);
        Optional<NonBlockingProvider> first = loader.findFirst();
        if (first.isEmpty()) {
            PROVIDER = null;
        } else {
            PROVIDER = first.get();
        }
    }

    /**
     * Delegates this call to the declared {@link NonBlockingProvider} implementation, hopefully Quarkus Vert.x, if
     * it is available.
     *
     * @throws RuntimeException if the Quarkus Vert.x extension is not present.
     * @see NonBlockingProvider#subscribeAndAwait(Supplier)
     */
    public static <T> T subscribeAndAwait(Supplier<Uni<T>> uniSupplier) throws Throwable {
        if (PROVIDER == null) {
            throw new RuntimeException(
                    ERROR_MSG);
        }
        return PROVIDER.subscribeAndAwait(uniSupplier);
    }
}
