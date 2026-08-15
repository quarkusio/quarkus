package io.quarkus.reactive.oracle.client;

import io.quarkus.reactive.datasource.PoolCreator;
import io.vertx.oracleclient.OracleConnectOptions;
import io.vertx.sqlclient.Pool;

/**
 * @deprecated Use {@link PoolCreator} instead.
 */
@Deprecated(forRemoval = true)
public interface OraclePoolCreator extends PoolCreator {

    Pool create(Input input);

    @Override
    default Pool create(PoolCreator.Input input) {
        return create((Input) input);
    }

    interface Input extends PoolCreator.Input {

        OracleConnectOptions oracleConnectOptions();
    }
}
