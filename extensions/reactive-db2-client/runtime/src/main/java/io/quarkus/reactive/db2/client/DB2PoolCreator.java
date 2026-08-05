package io.quarkus.reactive.db2.client;

import io.quarkus.reactive.datasource.PoolCreator;
import io.vertx.db2client.DB2ConnectOptions;
import io.vertx.sqlclient.Pool;

/**
 * @deprecated Use {@link PoolCreator} instead.
 */
@Deprecated(forRemoval = true)
public interface DB2PoolCreator extends PoolCreator {

    Pool create(Input input);

    @Override
    default Pool create(PoolCreator.Input input) {
        return create((Input) input);
    }

    interface Input extends PoolCreator.Input {

        DB2ConnectOptions db2ConnectOptions();
    }
}
