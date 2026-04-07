package io.quarkus.reactive.mysql.client;

import java.util.List;

import io.quarkus.reactive.datasource.PoolCreator;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;

/**
 * @deprecated Use {@link PoolCreator} instead.
 */
@Deprecated(forRemoval = true)
public interface MySQLPoolCreator extends PoolCreator {

    Pool create(Input input);

    @Override
    default Pool create(PoolCreator.Input input) {
        return create((Input) input);
    }

    interface Input extends PoolCreator.Input {

        List<MySQLConnectOptions> mySQLConnectOptionsList();
    }
}
