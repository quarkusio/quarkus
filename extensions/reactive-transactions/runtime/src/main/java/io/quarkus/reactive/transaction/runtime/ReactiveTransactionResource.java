package io.quarkus.reactive.transaction.runtime;

import io.vertx.core.Future;

/**
 * A tech-agnostic representation of a resource enlisted in a reactive transaction,
 * analogous to {@code javax.transaction.xa.XAResource} in JTA.
 * <p>
 * Only a single resource can be enlisted per reactive transaction (no XA support).
 */
public interface ReactiveTransactionResource {

    Future<Void> commit();

    Future<Void> rollback();

    Future<Void> close();
}
