package io.quarkus.reactive.pg.client;

import jakarta.inject.Singleton;

import io.quarkus.reactive.datasource.PoolCreator;
import io.vertx.pgclient.PgBuilder;
import io.vertx.sqlclient.Pool;

@Singleton
public class LocalhostPgPoolCreator implements PoolCreator {

    @Override
    public Pool create(Input input) {
        return PgBuilder.pool()
                .with(input.poolOptions())
                .connectingTo(() -> input.dynamicConnectOptionsList().get()
                        .map(options -> options.setHost("localhost").setPort(5431)))
                .using(input.vertx())
                .build();
    }
}
