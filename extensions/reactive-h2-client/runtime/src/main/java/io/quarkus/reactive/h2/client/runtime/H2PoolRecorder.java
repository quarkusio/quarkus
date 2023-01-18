package io.quarkus.reactive.h2.client.runtime;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.PoolOptions;

@Recorder
public class H2PoolRecorder {

    private static final Logger log = Logger.getLogger(H2PoolRecorder.class);

    public RuntimeValue<JDBCPool> configureH2Pool(RuntimeValue<Vertx> vertx,
            Supplier<Integer> eventLoopCount,
            String dataSourceName,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesReactiveRuntimeConfig dataSourcesReactiveRuntimeConfig,
            DataSourcesReactiveH2Config dataSourcesReactiveH2Config,
            ShutdownContext shutdown) {

        JDBCPool h2Pool = initialize(vertx.getValue(),
                eventLoopCount.get(),
                dataSourcesRuntimeConfig.getDataSourceRuntimeConfig(dataSourceName),
                dataSourcesReactiveRuntimeConfig.getDataSourceReactiveRuntimeConfig(dataSourceName),
                dataSourcesReactiveH2Config.getDataSourceReactiveRuntimeConfig(dataSourceName));

        shutdown.addShutdownTask(h2Pool::close);
        return new RuntimeValue<>(h2Pool);
    }

    public RuntimeValue<io.vertx.mutiny.jdbcclient.JDBCPool> mutinyH2Pool(RuntimeValue<JDBCPool> h2Pool) {
        return new RuntimeValue<>(io.vertx.mutiny.jdbcclient.JDBCPool.newInstance(h2Pool.getValue()));
    }

    private JDBCPool initialize(Vertx vertx,
            Integer eventLoopCount,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveH2Config dataSourceReactiveH2Config) {
        PoolOptions poolOptions = toPoolOptions(eventLoopCount, dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactiveH2Config);
        JDBCConnectOptions connectOptions = toH2ConnectOptions(dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactiveH2Config);
        if (dataSourceReactiveRuntimeConfig.threadLocal.isPresent()) {
            log.warn(
                    "Configuration element 'thread-local' on Reactive datasource connections is deprecated and will be ignored. The started pool will always be based on a per-thread separate pool now.");
        }
        return JDBCPool.pool(vertx, connectOptions, poolOptions);
    }

    private PoolOptions toPoolOptions(Integer eventLoopCount,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveH2Config dataSourceReactiveH2Config) {
        PoolOptions poolOptions = new PoolOptions();

        if (dataSourceReactiveRuntimeConfig.maxSize.isPresent()) {
            poolOptions.setMaxSize(dataSourceReactiveRuntimeConfig.maxSize.getAsInt());
        }

        if (dataSourceReactiveRuntimeConfig.idleTimeout.isPresent()) {
            int idleTimeout = Math.toIntExact(dataSourceReactiveRuntimeConfig.idleTimeout.get().toMillis());
            poolOptions.setIdleTimeout(idleTimeout).setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
        }

        if (dataSourceReactiveRuntimeConfig.shared) {
            poolOptions.setShared(true);
            dataSourceReactiveRuntimeConfig.name.ifPresent(poolOptions::setName);
        }

        if (dataSourceReactiveRuntimeConfig.eventLoopSize.isPresent()) {
            poolOptions.setEventLoopSize(Math.max(0, dataSourceReactiveRuntimeConfig.eventLoopSize.getAsInt()));
        } else if (eventLoopCount != null) {
            poolOptions.setEventLoopSize(Math.max(0, eventLoopCount));
        }

        if (dataSourceReactiveH2Config.connectionTimeout.isPresent()) {
            poolOptions.setConnectionTimeout(dataSourceReactiveH2Config.connectionTimeout.getAsInt());
            poolOptions.setConnectionTimeoutUnit(TimeUnit.SECONDS);
        }

        return poolOptions;
    }

    private JDBCConnectOptions toH2ConnectOptions(DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveH2Config dataSourceReactiveH2Config) {
        JDBCConnectOptions connectOptions = new JDBCConnectOptions();
        if (dataSourceReactiveRuntimeConfig.url.isPresent()) {
            String url = dataSourceReactiveRuntimeConfig.url.get();
            // clean up the URL to make migrations easier
            if (url.startsWith("vertx-reactive:h2:")) {
                url = url.substring("vertx-reactive:".length());
            }
            connectOptions.setJdbcUrl("jdbc:" + url);
        }

        dataSourceRuntimeConfig.username.ifPresent(connectOptions::setUser);
        dataSourceRuntimeConfig.password.ifPresent(connectOptions::setPassword);

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
                connectOptions.setPassword(password);
            }
        }

        return connectOptions;
    }
}
