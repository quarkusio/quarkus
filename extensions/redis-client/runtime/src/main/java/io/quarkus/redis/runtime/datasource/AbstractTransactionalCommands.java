package io.quarkus.redis.runtime.datasource;

import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.vertx.mutiny.redis.client.Response;

public class AbstractTransactionalCommands implements ReactiveTransactionalRedisCommands {

    protected final TransactionHolder tx;
    private final ReactiveTransactionalRedisDataSource ds;

    public AbstractTransactionalCommands(ReactiveTransactionalRedisDataSource ds, TransactionHolder tx) {
        this.ds = ds;
        this.tx = tx;
    }

    protected void queuedOrDiscard(Response response) {
        if (!"QUEUED".equals(response.toString())) {
            this.tx.discard();
            throw new IllegalStateException("Unable to add command to the current transaction");
        }
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return ds;
    }
}
