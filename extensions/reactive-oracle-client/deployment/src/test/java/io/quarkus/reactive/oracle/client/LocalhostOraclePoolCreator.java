package io.quarkus.reactive.oracle.client;

import jakarta.inject.Singleton;

import io.quarkus.reactive.datasource.PoolCreator;
import io.vertx.oracleclient.OracleBuilder;
import io.vertx.sqlclient.Pool;

@Singleton
public class LocalhostOraclePoolCreator implements PoolCreator {

    @Override
    public Pool create(Input input) {
        return OracleBuilder.pool()
                .with(input.poolOptions())
                .connectingTo(() -> input.dynamicConnectOptionsList().get()
                        .map(options -> options.setHost("localhost").setPort(1521)))
                .using(input.vertx())
                .build();
    }
}
