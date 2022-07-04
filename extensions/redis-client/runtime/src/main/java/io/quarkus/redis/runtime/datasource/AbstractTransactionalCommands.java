package io.quarkus.redis.runtime.datasource;

import io.vertx.mutiny.redis.client.Response;

public class AbstractTransactionalCommands {

    protected final TransactionHolder tx;

    public AbstractTransactionalCommands(TransactionHolder tx) {
        this.tx = tx;
    }

    protected void queuedOrDiscard(Response response) {
        if (!"QUEUED".equals(response.toString())) {
            this.tx.discard();
            throw new IllegalStateException("Unable to add command to the current transaction");
        }
    }
}
