package io.quarkus.test.hibernate.reactive.panache;

import java.util.function.Supplier;

import io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperations;
import io.quarkus.test.vertx.UniAsserter;
import io.quarkus.test.vertx.UniAsserterInterceptor;
import io.smallrye.mutiny.Uni;

/**
 * A {@link UniAsserterInterceptor} that wraps each assert method within a separate reactive transaction.
 *
 * @see UniAsserter
 */
public final class TransactionalUniAsserter extends UniAsserterInterceptor {

    TransactionalUniAsserter(UniAsserter asserter) {
        super(asserter);
    }

    @Override
    protected <T> Supplier<Uni<T>> transformUni(Supplier<Uni<T>> uniSupplier) {
        return () -> SessionOperations.withTransaction(uniSupplier);
    }

}