package io.quarkus.hibernate.reactive.panache.common.runtime;

import org.hibernate.reactive.mutiny.Mutiny.Transaction;

public class TestReactiveTransactionalInterceptor extends ReactiveTransactionalInterceptorBase {

    @Override
    protected void inTransactionCallback(Transaction tx) {
        tx.markForRollback();
    }

}
