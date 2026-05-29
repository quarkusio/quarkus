package io.quarkus.hibernate.reactive.panache.common.runtime;

import org.hibernate.reactive.mutiny.Mutiny;

import io.vertx.sqlclient.Transaction;

public class VertxTransactionWrapper implements Mutiny.Transaction {
    private final Transaction vertxTransaction;

    public VertxTransactionWrapper(Transaction vertxTransaction) {
        this.vertxTransaction = vertxTransaction;
    }

    @Override
    public void markForRollback() {
        vertxTransaction.rollback();
    }

    @Override
    public boolean isMarkedForRollback() {
        return false;
    }
}
