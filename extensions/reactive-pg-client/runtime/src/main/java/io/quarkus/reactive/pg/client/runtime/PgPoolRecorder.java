package io.quarkus.reactive.pg.client.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

@Recorder
public class PgPoolRecorder {

    public RuntimeValue<PgPool> configurePgPool(RuntimeValue<Vertx> vertx, BeanContainer container,
            DataSourceConfig dataSourceConfig, PgPoolConfig pgPoolConfig, ShutdownContext shutdown) {

        PgPool pgPool = initialize(vertx.getValue(), dataSourceConfig, pgPoolConfig);

        PgPoolProducer producer = container.instance(PgPoolProducer.class);
        producer.initialize(pgPool);

        shutdown.addShutdownTask(pgPool::close);
        return new RuntimeValue<>(pgPool);
    }

    private PgPool initialize(Vertx vertx, DataSourceConfig dataSourceConfig, PgPoolConfig pgPoolConfig) {
        PoolOptions poolOptions = toPoolOptions(dataSourceConfig, pgPoolConfig);
        PgConnectOptions pgConnectOptions = toPgConnectOptions(dataSourceConfig, pgPoolConfig);
        return PgPool.pool(vertx, pgConnectOptions, poolOptions);
    }

    private PoolOptions toPoolOptions(DataSourceConfig dataSourceConfig, PgPoolConfig pgPoolConfig) {
        PoolOptions poolOptions;
        poolOptions = new PoolOptions();
        if (dataSourceConfig != null) {
            dataSourceConfig.maxSize.ifPresent(value -> poolOptions.setMaxSize(value));
        }

        return poolOptions;
    }

    private PgConnectOptions toPgConnectOptions(DataSourceConfig dataSourceConfig, PgPoolConfig pgPoolConfig) {
        PgConnectOptions pgConnectOptions;
        if (dataSourceConfig != null) {
            pgConnectOptions = dataSourceConfig.url
                    .filter(s -> s.matches("^vertx-reactive:postgre(?:s|sql)://.*$"))
                    .map(s -> s.substring("vertx-reactive:".length()))
                    .map(PgConnectOptions::fromUri)
                    .orElse(new PgConnectOptions());

            dataSourceConfig.username.ifPresent(value -> pgConnectOptions.setUser(value));
            dataSourceConfig.password.ifPresent(value -> pgConnectOptions.setPassword(value));

        } else {
            pgConnectOptions = new PgConnectOptions();
        }

        if (pgPoolConfig != null) {
            pgPoolConfig.cachePreparedStatements.ifPresent(value -> pgConnectOptions.setCachePreparedStatements(value));
            pgPoolConfig.pipeliningLimit.ifPresent(value -> pgConnectOptions.setPipeliningLimit(value));
        }

        return pgConnectOptions;
    }
}
