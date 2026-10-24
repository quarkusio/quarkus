package io.quarkus.reactive.pg.client.runtime;

import static io.quarkus.reactive.datasource.runtime.ReactiveDataSourceUtil.qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;

import io.quarkus.arc.Arc;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.reactive.datasource.PoolCreator;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.ReactivePoolUtil;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.SslMode;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;

public final class PgPoolRecorder {

    private PgPoolRecorder() {
    }

    public static Optional<Pool> createPool(String dataSourceName, DataSourcesRuntimeConfig runtimeConfig,
            DataSourcesReactiveRuntimeConfig reactiveRuntimeConfig,
            DataSourcesReactivePostgreSQLConfig reactivePostgreRuntimeConfig, Vertx vertx, int eventLoopCount,
            TlsConfigurationRegistry tlsRegistry) {
        if (!ReactivePoolUtil.checkActive(dataSourceName, runtimeConfig, reactiveRuntimeConfig).value()) {
            return Optional.empty();
        }
        return Optional.of(initialize((VertxInternal) vertx,
                eventLoopCount,
                dataSourceName,
                runtimeConfig.dataSources().get(dataSourceName),
                reactiveRuntimeConfig.dataSources().get(dataSourceName).reactive(),
                reactivePostgreRuntimeConfig.dataSources().get(dataSourceName).reactive().postgresql(),
                tlsRegistry));
    }

    private static Pool initialize(VertxInternal vertx,
            Integer eventLoopCount,
            String dataSourceName,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig,
            TlsConfigurationRegistry tlsRegistry) {
        PoolOptions poolOptions = ReactivePoolUtil.toPoolOptions(eventLoopCount, dataSourceReactiveRuntimeConfig);
        List<PgConnectOptions> pgConnectOptionsList = toPgConnectOptions(dataSourceName, dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactivePostgreSQLConfig, tlsRegistry);
        Supplier<Future<SqlConnectOptions>> databasesSupplier = ReactivePoolUtil.toDatabasesSupplier(pgConnectOptionsList,
                dataSourceRuntimeConfig, PgConnectOptions::new);
        return createPool(vertx, poolOptions, pgConnectOptionsList, dataSourceName, databasesSupplier);
    }

    private static List<PgConnectOptions> toPgConnectOptions(String dataSourceName,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig,
            TlsConfigurationRegistry tlsRegistry) {
        List<PgConnectOptions> pgConnectOptionsList = new ArrayList<>();

        if (dataSourceReactiveRuntimeConfig.url().isPresent()) {
            List<String> urls = dataSourceReactiveRuntimeConfig.url().get();
            urls.forEach(url -> {
                // clean up the URL to make migrations easier
                if (url.startsWith("vertx-reactive:")) {
                    url = url.substring("vertx-reactive:".length());
                }
                pgConnectOptionsList.add(PgConnectOptions.fromUri(url));
            });
        } else {
            pgConnectOptionsList.add(new PgConnectOptions());
        }

        pgConnectOptionsList.forEach(pgConnectOptions -> {
            ReactivePoolUtil.configureCredentials(pgConnectOptions, dataSourceRuntimeConfig);

            ReactivePoolUtil.configurePreparedStatementCache(pgConnectOptions, dataSourceReactiveRuntimeConfig);

            if (dataSourceReactivePostgreSQLConfig.pipeliningLimit().isPresent()) {
                pgConnectOptions.setPipeliningLimit(dataSourceReactivePostgreSQLConfig.pipeliningLimit().getAsInt());
            }

            if (dataSourceReactivePostgreSQLConfig.sslMode().isPresent()) {
                final SslMode sslMode = dataSourceReactivePostgreSQLConfig.sslMode().get();
                pgConnectOptions.setSslMode(sslMode);

                var algo = dataSourceReactiveRuntimeConfig.hostnameVerificationAlgorithm();
                if ("NONE".equalsIgnoreCase(algo) && sslMode == SslMode.VERIFY_FULL) {
                    throw new IllegalArgumentException(
                            "quarkus.datasource.reactive.hostname-verification-algorithm must be specified under verify-full sslmode");
                }
            } else if (dataSourceReactiveRuntimeConfig.tlsConfigurationName().isPresent()) {
                // Auto-enable SSL mode when a named TLS configuration is set
                pgConnectOptions.setSslMode(SslMode.REQUIRE);
            }

            if (dataSourceReactivePostgreSQLConfig.sslNegotiation().isPresent()) {
                pgConnectOptions.setSslNegotiation(dataSourceReactivePostgreSQLConfig.sslNegotiation().get());
            }

            pgConnectOptions.setUseLayer7Proxy(dataSourceReactivePostgreSQLConfig.useLayer7Proxy());

            ReactivePoolUtil.configureSsl(pgConnectOptions, dataSourceReactiveRuntimeConfig, tlsRegistry);

            dataSourceReactiveRuntimeConfig.additionalProperties().forEach(pgConnectOptions::addProperty);

            // Use the convention defined by Quarkus Micrometer Vert.x metrics to create metrics prefixed with postgresql.
            // and the client_name as tag.
            // See io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractPrefix and
            // io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractClientName
            pgConnectOptions.setMetricsName("postgresql|" + dataSourceName);
        });

        return pgConnectOptionsList;
    }

    private static Pool createPool(Vertx vertx, PoolOptions poolOptions,
            List<PgConnectOptions> pgConnectOptionsList,
            String dataSourceName, Supplier<Future<SqlConnectOptions>> databases) {
        Instance<PoolCreator> instance = Arc.container().select(PoolCreator.class, qualifier(dataSourceName));
        if (instance.isResolvable()) {
            PoolCreator.Input input = new DefaultInput(vertx, poolOptions, pgConnectOptionsList);
            return instance.get().create(input);
        }

        return PgBuilder.pool().with(poolOptions)
                .connectingTo(databases)
                .using(vertx).build();
    }

    private static class DefaultInput implements PoolCreator.Input {
        private final Vertx vertx;
        private final PoolOptions poolOptions;
        private final List<? extends SqlConnectOptions> connectOptionsList;

        public DefaultInput(Vertx vertx, PoolOptions poolOptions, List<? extends SqlConnectOptions> connectOptionsList) {
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
            return (List<SqlConnectOptions>) connectOptionsList;
        }
    }
}
