package io.quarkus.reactive.mssql.client.runtime;

import static io.quarkus.reactive.datasource.runtime.ReactiveDataSourceUtil.qualifier;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.reactive.datasource.PoolCreator;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.ReactivePoolUtil;
import io.quarkus.reactive.mssql.client.MSSQLPoolCreator;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.mssqlclient.MSSQLBuilder;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;

public final class MSSQLPoolRecorder {

    private static final Logger log = Logger.getLogger(MSSQLPoolRecorder.class);

    private MSSQLPoolRecorder() {
    }

    public static Optional<Pool> createPool(String dataSourceName, DataSourcesRuntimeConfig runtimeConfig,
            DataSourcesReactiveRuntimeConfig reactiveRuntimeConfig,
            DataSourcesReactiveMSSQLConfig reactiveMSSQLRuntimeConfig, Vertx vertx, int eventLoopCount,
            TlsConfigurationRegistry tlsRegistry) {
        if (!ReactivePoolUtil.checkActive(dataSourceName, runtimeConfig, reactiveRuntimeConfig).value()) {
            return Optional.empty();
        }
        return Optional.of(initialize((VertxInternal) vertx,
                eventLoopCount,
                dataSourceName,
                runtimeConfig.dataSources().get(dataSourceName),
                reactiveRuntimeConfig.dataSources().get(dataSourceName).reactive(),
                reactiveMSSQLRuntimeConfig.dataSources().get(dataSourceName).reactive().mssql(),
                tlsRegistry));
    }

    private static Pool initialize(VertxInternal vertx,
            Integer eventLoopCount,
            String dataSourceName,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMSSQLConfig dataSourceReactiveMSSQLConfig,
            TlsConfigurationRegistry tlsRegistry) {
        PoolOptions poolOptions = ReactivePoolUtil.toPoolOptions(eventLoopCount, dataSourceReactiveRuntimeConfig);
        MSSQLConnectOptions mssqlConnectOptions = toMSSQLConnectOptions(dataSourceName, dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactiveMSSQLConfig, tlsRegistry);
        Supplier<Future<SqlConnectOptions>> databasesSupplier = ReactivePoolUtil.toDatabasesSupplier(
                List.of(mssqlConnectOptions), dataSourceRuntimeConfig, MSSQLConnectOptions::new);
        return createPool(vertx, poolOptions, mssqlConnectOptions, dataSourceName, databasesSupplier);
    }

    private static MSSQLConnectOptions toMSSQLConnectOptions(String dataSourceName, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMSSQLConfig dataSourceReactiveMSSQLConfig,
            TlsConfigurationRegistry tlsRegistry) {
        MSSQLConnectOptions mssqlConnectOptions;
        if (dataSourceReactiveRuntimeConfig.url().isPresent()) {
            List<String> urls = dataSourceReactiveRuntimeConfig.url().get();
            if (urls.size() > 1) {
                log.warn("The Reactive MSSQL client does not support multiple URLs. The first one will be used, and " +
                        "others will be ignored.");
            }
            String url = urls.get(0);
            // clean up the URL to make migrations easier
            if (url.startsWith("vertx-reactive:")) {
                url = url.substring("vertx-reactive:".length());
            }
            mssqlConnectOptions = MSSQLConnectOptions.fromUri(url);
        } else {
            mssqlConnectOptions = new MSSQLConnectOptions();
        }

        if (dataSourceReactiveMSSQLConfig.packetSize().isPresent()) {
            mssqlConnectOptions.setPacketSize(dataSourceReactiveMSSQLConfig.packetSize().getAsInt());
        }

        ReactivePoolUtil.configureCredentials(mssqlConnectOptions, dataSourceRuntimeConfig);

        if (dataSourceReactiveMSSQLConfig.encryptionMode().isPresent()) {
            mssqlConnectOptions.setEncryptionMode(dataSourceReactiveMSSQLConfig.encryptionMode().get());
        } else if (dataSourceReactiveMSSQLConfig.ssl()) {
            mssqlConnectOptions.setSsl(true);
        } else if (dataSourceReactiveRuntimeConfig.tlsConfigurationName().isPresent()) {
            // Auto-enable SSL when a named TLS configuration is set
            mssqlConnectOptions.setSsl(true);
        }

        ReactivePoolUtil.configureSsl(mssqlConnectOptions, dataSourceReactiveRuntimeConfig, tlsRegistry);

        dataSourceReactiveRuntimeConfig.additionalProperties().forEach(mssqlConnectOptions::addProperty);

        // Use the convention defined by Quarkus Micrometer Vert.x metrics to create metrics prefixed with mssql.
        // with the client_name as tag.
        mssqlConnectOptions.setMetricsName("mssql|" + dataSourceName);

        return mssqlConnectOptions;
    }

    private static Pool createPool(Vertx vertx, PoolOptions poolOptions, MSSQLConnectOptions mssqlConnectOptions,
            String dataSourceName, Supplier<Future<SqlConnectOptions>> databases) {
        Instance<PoolCreator> instance = Arc.container().select(PoolCreator.class, qualifier(dataSourceName));
        if (instance.isResolvable()) {
            MSSQLPoolCreator.Input input = new DefaultInput(vertx, poolOptions, mssqlConnectOptions);
            return instance.get().create(input);
        }
        return MSSQLBuilder.pool().with(poolOptions)
                .connectingTo(databases)
                .using(vertx).build();
    }

    private static class DefaultInput implements MSSQLPoolCreator.Input {
        private final Vertx vertx;
        private final PoolOptions poolOptions;
        private final MSSQLConnectOptions connectOptions;

        public DefaultInput(Vertx vertx, PoolOptions poolOptions, MSSQLConnectOptions connectOptions) {
            this.vertx = vertx;
            this.poolOptions = poolOptions;
            this.connectOptions = connectOptions;
        }

        @Override
        public Vertx vertx() {
            return vertx;
        }

        @Override
        public PoolOptions poolOptions() {
            return poolOptions;
        }

        @Override
        public List<SqlConnectOptions> connectOptionsList() {
            return List.of(connectOptions);
        }

        @Override
        public MSSQLConnectOptions msSQLConnectOptions() {
            return connectOptions;
        }
    }
}
