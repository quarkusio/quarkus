package io.quarkus.reactive.db2.client.runtime;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;

import java.util.Map;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;
import io.vertx.db2client.DB2ConnectOptions;
import io.vertx.db2client.DB2Pool;
import io.vertx.sqlclient.PoolOptions;

@Recorder
public class DB2PoolRecorder {

    public RuntimeValue<DB2Pool> configureDB2Pool(RuntimeValue<Vertx> vertx, BeanContainer container,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveDB2Config dataSourceReactiveDB2Config,
            ShutdownContext shutdown) {

        DB2Pool pool = initialize(vertx.getValue(), dataSourcesRuntimeConfig.defaultDataSource,
                dataSourceReactiveRuntimeConfig,
                dataSourceReactiveDB2Config);

        DB2PoolProducer producer = container.instance(DB2PoolProducer.class);
        producer.initialize(pool);

        shutdown.addShutdownTask(pool::close);
        return new RuntimeValue<>(pool);
    }

    private DB2Pool initialize(Vertx vertx, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveDB2Config dataSourceReactiveDB2Config) {
        PoolOptions poolOptions = toPoolOptions(dataSourceRuntimeConfig, dataSourceReactiveRuntimeConfig,
                dataSourceReactiveDB2Config);
        DB2ConnectOptions connectOptions = toConnectOptions(dataSourceRuntimeConfig, dataSourceReactiveRuntimeConfig,
                dataSourceReactiveDB2Config);
        if (dataSourceReactiveRuntimeConfig.threadLocal.isPresent() &&
                dataSourceReactiveRuntimeConfig.threadLocal.get()) {
            return new ThreadLocalDB2Pool(vertx, connectOptions, poolOptions);
        }
        return DB2Pool.pool(vertx, connectOptions, poolOptions);
    }

    private PoolOptions toPoolOptions(DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveDB2Config dataSourceReactiveDB2Config) {
        PoolOptions poolOptions;
        poolOptions = new PoolOptions();

        if (dataSourceReactiveRuntimeConfig.maxSize.isPresent()) {
            poolOptions.setMaxSize(dataSourceReactiveRuntimeConfig.maxSize.getAsInt());
        }

        return poolOptions;
    }

    private DB2ConnectOptions toConnectOptions(DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveDB2Config dataSourceReactiveDB2Config) {
        DB2ConnectOptions connectOptions;

        if (dataSourceReactiveRuntimeConfig.url.isPresent()) {
            String url = dataSourceReactiveRuntimeConfig.url.get();
            // clean up the URL to make migrations easier
            if (url.matches("^vertx-reactive:db2://.*$")) {
                url = url.substring("vertx-reactive:".length());
            }
            connectOptions = DB2ConnectOptions.fromUri(url);
        } else {
            connectOptions = new DB2ConnectOptions();
        }

        if (dataSourceRuntimeConfig.username.isPresent()) {
            connectOptions.setUser(dataSourceRuntimeConfig.username.get());
        }

        if (dataSourceRuntimeConfig.password.isPresent()) {
            connectOptions.setPassword(dataSourceRuntimeConfig.password.get());
        }

        // credentials provider
        if (dataSourceRuntimeConfig.credentialsProvider.isPresent()) {
            String beanName = dataSourceRuntimeConfig.credentialsProviderName.orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = dataSourceRuntimeConfig.credentialsProvider.get();
            Map<String, String> credentials = credentialsProvider.getCredentials(name);
            String user = credentials.get(USER_PROPERTY_NAME);
            String password = credentials.get(PASSWORD_PROPERTY_NAME);
            if (user != null) {
                connectOptions.setUser(user);
            }
            if (password != null) {
                connectOptions.setPassword(user);
            }
        }

        if (dataSourceReactiveDB2Config.cachePreparedStatements.isPresent()) {
            connectOptions.setCachePreparedStatements(dataSourceReactiveDB2Config.cachePreparedStatements.get());
        }

        return connectOptions;
    }
}
