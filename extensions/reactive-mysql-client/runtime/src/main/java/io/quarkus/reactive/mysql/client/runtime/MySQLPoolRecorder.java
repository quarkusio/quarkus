package io.quarkus.reactive.mysql.client.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;

@Recorder
public class MySQLPoolRecorder {

    public RuntimeValue<MySQLPool> configureMySQLPool(RuntimeValue<Vertx> vertx, BeanContainer container,
            DataSourceConfig dataSourceConfig, MySQLPoolConfig mysqlPoolConfig, ShutdownContext shutdown) {

        MySQLPool mysqlPool = initialize(vertx.getValue(), dataSourceConfig, mysqlPoolConfig);

        MySQLPoolProducer producer = container.instance(MySQLPoolProducer.class);
        producer.initialize(mysqlPool);

        shutdown.addShutdownTask(mysqlPool::close);
        return new RuntimeValue<>(mysqlPool);
    }

    private MySQLPool initialize(Vertx vertx, DataSourceConfig dataSourceConfig, MySQLPoolConfig mysqlPoolConfig) {
        PoolOptions poolOptions = toPoolOptions(dataSourceConfig, mysqlPoolConfig);
        MySQLConnectOptions mysqlConnectOptions = toMySQLConnectOptions(dataSourceConfig, mysqlPoolConfig);
        return MySQLPool.pool(vertx, mysqlConnectOptions, poolOptions);
    }

    private PoolOptions toPoolOptions(DataSourceConfig dataSourceConfig, MySQLPoolConfig mysqlPoolConfig) {
        PoolOptions poolOptions;
        poolOptions = new PoolOptions();
        if (dataSourceConfig != null) {
            dataSourceConfig.maxSize.ifPresent(value -> poolOptions.setMaxSize(value));
        }

        return poolOptions;
    }

    private MySQLConnectOptions toMySQLConnectOptions(DataSourceConfig dataSourceConfig, MySQLPoolConfig mysqlPoolConfig) {
        MySQLConnectOptions mysqlConnectOptions;
        if (dataSourceConfig != null) {
            mysqlConnectOptions = dataSourceConfig.url
                    .filter(s -> s.startsWith("vertx-reactive:mysql://"))
                    .map(s -> s.substring("vertx-reactive:".length()))
                    .map(MySQLConnectOptions::fromUri)
                    .orElse(new MySQLConnectOptions());

            dataSourceConfig.username.ifPresent(value -> mysqlConnectOptions.setUser(value));
            dataSourceConfig.password.ifPresent(value -> mysqlConnectOptions.setPassword(value));

        } else {
            mysqlConnectOptions = new MySQLConnectOptions();
        }

        if (mysqlPoolConfig != null) {
            mysqlPoolConfig.cachePreparedStatements.ifPresent(value -> mysqlConnectOptions.setCachePreparedStatements(value));
            mysqlPoolConfig.charset.ifPresent(value -> mysqlConnectOptions.setCharset(value));
            mysqlPoolConfig.collation.ifPresent(value -> mysqlConnectOptions.setCollation(value));
        }

        return mysqlConnectOptions;
    }
}
