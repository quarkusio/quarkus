package io.quarkus.reactive.pg.client;

import io.quarkus.reactive.datasource.PoolCreator;

/**
 * @deprecated Use {@link PoolCreator} instead.
 */
@Deprecated(forRemoval = true)
public interface PgPoolCreator extends PoolCreator {
}
