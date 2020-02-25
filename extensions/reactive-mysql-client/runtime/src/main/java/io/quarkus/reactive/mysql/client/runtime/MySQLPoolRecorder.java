package io.quarkus.reactive.mysql.client.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
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
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig,
            ShutdownContext shutdown) {

        MySQLPool mysqlPool = initialize(vertx.getValue(), dataSourcesRuntimeConfig.defaultDataSource,
                dataSourceReactiveRuntimeConfig,
                dataSourceReactiveMySQLConfig);

        MySQLPoolProducer producer = container.instance(MySQLPoolProducer.class);
        producer.initialize(mysqlPool);

        shutdown.addShutdownTask(mysqlPool::close);
        return new RuntimeValue<>(mysqlPool);
    }

    private MySQLPool initialize(Vertx vertx, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig) {
        PoolOptions poolOptions = toPoolOptions(dataSourceRuntimeConfig, dataSourceReactiveRuntimeConfig,
                dataSourceReactiveMySQLConfig);
        MySQLConnectOptions mysqlConnectOptions = toMySQLConnectOptions(dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactiveMySQLConfig);
        return MySQLPool.pool(vertx, mysqlConnectOptions, poolOptions);
    }

    private PoolOptions toPoolOptions(DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig) {
        PoolOptions poolOptions;
        poolOptions = new PoolOptions();

        if (dataSourceReactiveRuntimeConfig.maxSize.isPresent()) {
            poolOptions.setMaxSize(dataSourceReactiveRuntimeConfig.maxSize.getAsInt());
        }

        return poolOptions;
    }

    private MySQLConnectOptions toMySQLConnectOptions(DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig) {
        MySQLConnectOptions mysqlConnectOptions;
        if (dataSourceReactiveRuntimeConfig.url.isPresent()) {
            String url = dataSourceReactiveRuntimeConfig.url.get();
            // clean up the URL to make migrations easier
            if (url.startsWith("vertx-reactive:mysql://")) {
                url = url.substring("vertx-reactive:".length());
            }
            mysqlConnectOptions = MySQLConnectOptions.fromUri(url);
        } else {
            mysqlConnectOptions = new MySQLConnectOptions();
        }

        if (dataSourceRuntimeConfig.username.isPresent()) {
            mysqlConnectOptions.setUser(dataSourceRuntimeConfig.username.get());
        }

        if (dataSourceRuntimeConfig.password.isPresent()) {
            mysqlConnectOptions.setPassword(dataSourceRuntimeConfig.password.get());
        }

        if (dataSourceReactiveMySQLConfig.cachePreparedStatements.isPresent()) {
            mysqlConnectOptions.setCachePreparedStatements(dataSourceReactiveMySQLConfig.cachePreparedStatements.get());
        }
        if (dataSourceReactiveMySQLConfig.charset.isPresent()) {
            mysqlConnectOptions.setCharset(dataSourceReactiveMySQLConfig.charset.get());
        }
        if (dataSourceReactiveMySQLConfig.collation.isPresent()) {
            mysqlConnectOptions.setCollation(dataSourceReactiveMySQLConfig.collation.get());
        }

        return mysqlConnectOptions;
    }
}
