package io.quarkus.reactive.mysql.client.runtime;

import static io.quarkus.reactive.datasource.runtime.ReactiveDataSourceUtil.qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.reactive.datasource.PoolCreator;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.ReactivePoolUtil;
import io.quarkus.reactive.mysql.client.MySQLPoolCreator;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.internal.VertxInternal;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.SslMode;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;

public final class MySQLPoolRecorder {

    private static final Logger log = Logger.getLogger(MySQLPoolRecorder.class);

    private MySQLPoolRecorder() {
    }

    public static Optional<Pool> createPool(String dataSourceName, DataSourcesRuntimeConfig runtimeConfig,
            DataSourcesReactiveRuntimeConfig reactiveRuntimeConfig,
            DataSourcesReactiveMySQLConfig reactiveMySQLRuntimeConfig, Vertx vertx, int eventLoopCount,
            TlsConfigurationRegistry tlsRegistry) {
        if (!ReactivePoolUtil.checkActive(dataSourceName, runtimeConfig, reactiveRuntimeConfig).value()) {
            return Optional.empty();
        }
        return Optional.of(initialize((VertxInternal) vertx,
                eventLoopCount,
                dataSourceName,
                runtimeConfig.dataSources().get(dataSourceName),
                reactiveRuntimeConfig.dataSources().get(dataSourceName).reactive(),
                reactiveMySQLRuntimeConfig.dataSources().get(dataSourceName).reactive().mysql(),
                tlsRegistry));
    }

    private static Pool initialize(VertxInternal vertx,
            Integer eventLoopCount,
            String dataSourceName,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig,
            TlsConfigurationRegistry tlsRegistry) {
        PoolOptions poolOptions = ReactivePoolUtil.toPoolOptions(eventLoopCount, dataSourceReactiveRuntimeConfig);
        if (dataSourceReactiveRuntimeConfig.connectionTimeout().isEmpty()
                && dataSourceReactiveMySQLConfig.connectionTimeout().isPresent()) {
            logWarningForDeprecatedConnectionTimeout(dataSourceName);
            poolOptions.setConnectionTimeout(dataSourceReactiveMySQLConfig.connectionTimeout().getAsInt());
            poolOptions.setConnectionTimeoutUnit(TimeUnit.SECONDS);
        }

        List<MySQLConnectOptions> mysqlConnectOptionsList = toMySQLConnectOptions(dataSourceName, dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactiveMySQLConfig, tlsRegistry);
        Supplier<Future<SqlConnectOptions>> databasesSupplier = ReactivePoolUtil.toDatabasesSupplier(mysqlConnectOptionsList,
                dataSourceRuntimeConfig, MySQLConnectOptions::new);
        return createPool(vertx, poolOptions, mysqlConnectOptionsList, dataSourceName, databasesSupplier);
    }

    private static void logWarningForDeprecatedConnectionTimeout(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            log.warn("'quarkus.datasource.reactive.mysql.connection-timeout' is deprecated,"
                    + " use 'quarkus.datasource.reactive.connection-timeout' instead");
        } else {
            log.warnf("'quarkus.datasource.\"%s\".reactive.mysql.connection-timeout' is deprecated,"
                    + " use 'quarkus.datasource.\"%s\".reactive.connection-timeout' instead",
                    dataSourceName, dataSourceName);
        }
    }

    private static List<MySQLConnectOptions> toMySQLConnectOptions(String dataSourceName,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig,
            TlsConfigurationRegistry tlsRegistry) {
        List<MySQLConnectOptions> mysqlConnectOptionsList = new ArrayList<>();
        if (dataSourceReactiveRuntimeConfig.url().isPresent()) {
            List<String> urls = dataSourceReactiveRuntimeConfig.url().get();
            urls.forEach(url -> {
                // clean up the URL to make migrations easier
                if (url.startsWith("vertx-reactive:")) {
                    url = url.substring("vertx-reactive:".length());
                }
                mysqlConnectOptionsList.add(MySQLConnectOptions.fromUri(url));
            });
        } else {
            mysqlConnectOptionsList.add(new MySQLConnectOptions());
        }

        mysqlConnectOptionsList.forEach(mysqlConnectOptions -> {
            ReactivePoolUtil.configureCredentials(mysqlConnectOptions, dataSourceRuntimeConfig);

            ReactivePoolUtil.configurePreparedStatementCache(mysqlConnectOptions, dataSourceReactiveRuntimeConfig);

            dataSourceReactiveMySQLConfig.charset().ifPresent(mysqlConnectOptions::setCharset);
            dataSourceReactiveMySQLConfig.collation().ifPresent(mysqlConnectOptions::setCollation);
            if (dataSourceReactiveMySQLConfig.characterEncoding().isPresent()) {
                mysqlConnectOptions.setCharacterEncoding(dataSourceReactiveMySQLConfig.characterEncoding().get());
            }
            if (dataSourceReactiveMySQLConfig.serverRsaPublicKeyPath().isPresent()) {
                mysqlConnectOptions.setServerRsaPublicKeyPath(dataSourceReactiveMySQLConfig.serverRsaPublicKeyPath().get());
            }
            if (dataSourceReactiveMySQLConfig.serverRsaPublicKeyValue().isPresent()) {
                mysqlConnectOptions
                        .setServerRsaPublicKeyValue(
                                Buffer.buffer(dataSourceReactiveMySQLConfig.serverRsaPublicKeyValue().get()));
            }

            if (dataSourceReactiveMySQLConfig.pipeliningLimit().isPresent()) {
                mysqlConnectOptions.setPipeliningLimit(dataSourceReactiveMySQLConfig.pipeliningLimit().getAsInt());
            }

            dataSourceReactiveMySQLConfig.useAffectedRows().ifPresent(mysqlConnectOptions::setUseAffectedRows);

            if (dataSourceReactiveMySQLConfig.sslMode().isPresent()) {
                final SslMode sslMode = dataSourceReactiveMySQLConfig.sslMode().get();
                mysqlConnectOptions.setSslMode(sslMode);

                var algo = dataSourceReactiveRuntimeConfig.hostnameVerificationAlgorithm();
                if ("NONE".equalsIgnoreCase(algo) && sslMode == SslMode.VERIFY_IDENTITY) {
                    throw new IllegalArgumentException(
                            "quarkus.datasource.reactive.hostname-verification-algorithm must be specified under verify-identity sslmode");
                }
            } else if (dataSourceReactiveRuntimeConfig.tlsConfigurationName().isPresent()) {
                // Auto-enable SSL mode when a named TLS configuration is set
                mysqlConnectOptions.setSslMode(SslMode.REQUIRED);
            }

            dataSourceReactiveMySQLConfig.authenticationPlugin().ifPresent(mysqlConnectOptions::setAuthenticationPlugin);

            ReactivePoolUtil.configureSsl(mysqlConnectOptions, dataSourceReactiveRuntimeConfig, tlsRegistry);

            dataSourceReactiveRuntimeConfig.additionalProperties().forEach(mysqlConnectOptions::addProperty);

            // Use the convention defined by Quarkus Micrometer Vert.x metrics to create metrics prefixed with mysql.
            // and the client_name as tag.
            // See io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractPrefix and
            // io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractClientName
            mysqlConnectOptions.setMetricsName("mysql|" + dataSourceName);
        });

        return mysqlConnectOptionsList;
    }

    private static Pool createPool(Vertx vertx, PoolOptions poolOptions,
            List<MySQLConnectOptions> mysqlConnectOptionsList,
            String dataSourceName, Supplier<Future<SqlConnectOptions>> databases) {
        Instance<PoolCreator> instance = Arc.container().select(PoolCreator.class, qualifier(dataSourceName));
        if (instance.isResolvable()) {
            PoolCreator.Input input = new DefaultInput(vertx, poolOptions, mysqlConnectOptionsList);
            return instance.get().create(input);
        }

        return MySQLBuilder.pool().with(poolOptions)
                .connectingTo(databases)
                .using(vertx).build();
    }

    private static class DefaultInput implements MySQLPoolCreator.Input {
        private final Vertx vertx;
        private final PoolOptions poolOptions;
        private final List<MySQLConnectOptions> connectOptionsList;

        public DefaultInput(Vertx vertx, PoolOptions poolOptions, List<MySQLConnectOptions> connectOptionsList) {
            this.vertx = vertx;
            this.poolOptions = poolOptions;
            this.connectOptionsList = connectOptionsList;
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
        @SuppressWarnings("unchecked")
        public List<SqlConnectOptions> connectOptionsList() {
            return (List<SqlConnectOptions>) (List<?>) connectOptionsList;
        }

        @Override
        public List<MySQLConnectOptions> mySQLConnectOptionsList() {
            return connectOptionsList;
        }
    }
}
