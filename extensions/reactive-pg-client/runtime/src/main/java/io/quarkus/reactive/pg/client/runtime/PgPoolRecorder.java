package io.quarkus.reactive.pg.client.runtime;

import static io.quarkus.reactive.datasource.runtime.ReactiveDataSourceUtil.qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.reactive.datasource.PoolCreator;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.ReactivePoolUtil;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.SslMode;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;

@Recorder
public class PgPoolRecorder {

    private static final boolean SUPPORTS_CACHE_PREPARED_STATEMENTS = true;

    private static final TypeLiteral<Instance<PoolCreator>> POOL_CREATOR_TYPE_LITERAL = new TypeLiteral<>() {
    };

    private final RuntimeValue<DataSourcesRuntimeConfig> runtimeConfig;
    private final RuntimeValue<DataSourcesReactiveRuntimeConfig> reactiveRuntimeConfig;
    private final RuntimeValue<DataSourcesReactivePostgreSQLConfig> reactivePostgreRuntimeConfig;

    public PgPoolRecorder(
            final RuntimeValue<DataSourcesRuntimeConfig> runtimeConfig,
            final RuntimeValue<DataSourcesReactiveRuntimeConfig> reactiveRuntimeConfig,
            final RuntimeValue<DataSourcesReactivePostgreSQLConfig> reactivePostgreRuntimeConfig) {
        this.runtimeConfig = runtimeConfig;
        this.reactiveRuntimeConfig = reactiveRuntimeConfig;
        this.reactivePostgreRuntimeConfig = reactivePostgreRuntimeConfig;
    }

    public Function<SyntheticCreationalContext<Pool>, Pool> configurePgPool(RuntimeValue<Vertx> vertx,
            Supplier<Integer> eventLoopCount, String dataSourceName, ShutdownContext shutdown) {
        return new Function<>() {
            @Override
            public Pool apply(SyntheticCreationalContext<Pool> context) {
                Pool pool = initialize((VertxInternal) vertx.getValue(),
                        eventLoopCount.get(),
                        dataSourceName,
                        runtimeConfig.getValue().dataSources().get(dataSourceName),
                        reactiveRuntimeConfig.getValue().dataSources().get(dataSourceName).reactive(),
                        reactivePostgreRuntimeConfig.getValue().dataSources().get(dataSourceName).reactive().postgresql(),
                        context);

                shutdown.addShutdownTask(pool::close);
                return pool;
            }
        };
    }

    private Pool initialize(VertxInternal vertx,
            Integer eventLoopCount,
            String dataSourceName,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig,
            SyntheticCreationalContext<Pool> context) {
        PoolOptions poolOptions = ReactivePoolUtil.toPoolOptions(eventLoopCount, dataSourceReactiveRuntimeConfig);
        List<PgConnectOptions> pgConnectOptionsList = toPgConnectOptions(dataSourceName, dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactivePostgreSQLConfig);
        Supplier<Future<SqlConnectOptions>> databasesSupplier = ReactivePoolUtil.toDatabasesSupplier(pgConnectOptionsList,
                dataSourceRuntimeConfig, PgConnectOptions::new);
        return createPool(vertx, poolOptions, pgConnectOptionsList, dataSourceName, databasesSupplier, context);
    }

    private List<PgConnectOptions> toPgConnectOptions(String dataSourceName, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig) {
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

            pgConnectOptions.setCachePreparedStatements(
                    dataSourceReactiveRuntimeConfig.cachePreparedStatements().orElse(SUPPORTS_CACHE_PREPARED_STATEMENTS));

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
            }

            pgConnectOptions.setUseLayer7Proxy(dataSourceReactivePostgreSQLConfig.useLayer7Proxy());

            ReactivePoolUtil.configureSsl(pgConnectOptions, dataSourceReactiveRuntimeConfig);

            dataSourceReactiveRuntimeConfig.additionalProperties().forEach(pgConnectOptions::addProperty);

            // Use the convention defined by Quarkus Micrometer Vert.x metrics to create metrics prefixed with postgresql.
            // and the client_name as tag.
            // See io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractPrefix and
            // io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractClientName
            pgConnectOptions.setMetricsName("postgresql|" + dataSourceName);
        });

        return pgConnectOptionsList;
    }

    private Pool createPool(Vertx vertx, PoolOptions poolOptions,
            List<PgConnectOptions> pgConnectOptionsList,
            String dataSourceName, Supplier<Future<SqlConnectOptions>> databases,
            SyntheticCreationalContext<Pool> context) {
        Instance<PoolCreator> instance = context.getInjectedReference(POOL_CREATOR_TYPE_LITERAL, qualifier(dataSourceName));
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
