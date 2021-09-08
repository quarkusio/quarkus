package io.quarkus.oidc;

import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;

/**
 * OIDC Context that can be used to run blocking OIDC tasks.
 */
public interface OidcContext<T> {
    Uni<T> runBlocking(Supplier<T> function);
}
