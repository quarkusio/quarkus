package io.quarkus.reactive.mssql.client;

import io.quarkus.reactive.datasource.PoolCreator;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.sqlclient.Pool;

/**
 * @deprecated Use {@link PoolCreator} instead.
 */
@Deprecated(forRemoval = true)
public interface MSSQLPoolCreator extends PoolCreator {

    Pool create(Input input);

    @Override
    default Pool create(PoolCreator.Input input) {
        return create((Input) input);
    }

    interface Input extends PoolCreator.Input {

        MSSQLConnectOptions msSQLConnectOptions();
    }
}
