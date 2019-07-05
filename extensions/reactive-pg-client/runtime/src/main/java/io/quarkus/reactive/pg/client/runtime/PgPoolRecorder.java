package io.quarkus.reactive.pg.client.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.vertx.core.Vertx;

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
        PgPoolOptions pgPoolOptions = toPgPoolOptions(dataSourceConfig, pgPoolConfig);
        return PgClient.pool(vertx, pgPoolOptions);
    }

    private PgPoolOptions toPgPoolOptions(DataSourceConfig dataSourceConfig, PgPoolConfig pgPoolConfig) {
        PgPoolOptions pgPoolOptions;
        if (dataSourceConfig != null) {

            pgPoolOptions = dataSourceConfig.url
                    .filter(s -> s.matches("^vertx-reactive:postgre(?:s|sql)://.*$"))
                    .map(s -> s.substring("vertx-reactive:".length()))
                    .map(PgPoolOptions::fromUri)
                    .orElse(new PgPoolOptions());

            dataSourceConfig.username.ifPresent(value -> pgPoolOptions.setUser(value));
            dataSourceConfig.password.ifPresent(value -> pgPoolOptions.setPassword(value));
            dataSourceConfig.maxSize.ifPresent(value -> pgPoolOptions.setMaxSize(value));

        } else {
            pgPoolOptions = new PgPoolOptions();
        }

        if (pgPoolConfig != null) {
            pgPoolConfig.cachePreparedStatements.ifPresent(value -> pgPoolOptions.setCachePreparedStatements(value));
            pgPoolConfig.pipeliningLimit.ifPresent(value -> pgPoolOptions.setPipeliningLimit(value));
        }

        return pgPoolOptions;
    }
}
