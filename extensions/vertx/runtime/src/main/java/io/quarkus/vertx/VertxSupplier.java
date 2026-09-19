package io.quarkus.vertx;

import java.util.function.Supplier;

import io.vertx.core.Vertx;

/**
 * A typed supplier of the Quarkus-managed {@link Vertx} instance.
 * <p>
 * This interface exists to provide a non-generic service type for the
 * Vert.x supplier in the service dependency graph, since {@code Supplier<Vertx>}
 * loses its type parameter at runtime due to erasure.
 * <p>
 * The supplier performs lazy initialization: the {@link Vertx} instance is
 * created on the first call to {@link #get()} and cached thereafter.
 */
public interface VertxSupplier extends Supplier<Vertx> {
}
