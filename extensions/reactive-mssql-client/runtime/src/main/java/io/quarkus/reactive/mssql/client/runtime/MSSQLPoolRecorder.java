package io.quarkus.reactive.mssql.client.runtime;

import static io.quarkus.reactive.datasource.runtime.ReactiveDataSourceUtil.qualifier;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import org.jboss.logging.Logger;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.reactive.datasource.PoolCreator;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.ReactivePoolUtil;
import io.quarkus.reactive.mssql.client.MSSQLPoolCreator;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.mssqlclient.MSSQLBuilder;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;

@Recorder
public class MSSQLPoolRecorder {

    private static final Logger log = Logger.getLogger(MSSQLPoolRecorder.class);

    private static final TypeLiteral<Instance<PoolCreator>> POOL_CREATOR_TYPE_LITERAL = new TypeLiteral<>() {
    };

    private final RuntimeValue<DataSourcesRuntimeConfig> runtimeConfig;
    private final RuntimeValue<DataSourcesReactiveRuntimeConfig> reactiveRuntimeConfig;
    private final RuntimeValue<DataSourcesReactiveMSSQLConfig> reactiveMSSQLRuntimeConfig;

    public MSSQLPoolRecorder(
            final RuntimeValue<DataSourcesRuntimeConfig> runtimeConfig,
            final RuntimeValue<DataSourcesReactiveRuntimeConfig> reactiveRuntimeConfig,
            final RuntimeValue<DataSourcesReactiveMSSQLConfig> reactiveMSSQLRuntimeConfig) {
        this.runtimeConfig = runtimeConfig;
        this.reactiveRuntimeConfig = reactiveRuntimeConfig;
        this.reactiveMSSQLRuntimeConfig = reactiveMSSQLRuntimeConfig;
    }

    public Function<SyntheticCreationalContext<Pool>, Pool> configureMSSQLPool(RuntimeValue<Vertx> vertx,
            Supplier<Integer> eventLoopCount, String dataSourceName, ShutdownContext shutdown,
            Supplier<TlsConfigurationRegistry> tlsRegistrySupplier) {
        return new Function<>() {
            @Override
            public Pool apply(SyntheticCreationalContext<Pool> context) {
                Pool pool = initialize((VertxInternal) vertx.getValue(),
                        eventLoopCount.get(),
                        dataSourceName,
                        runtimeConfig.getValue().dataSources().get(dataSourceName),
                        reactiveRuntimeConfig.getValue().dataSources().get(dataSourceName).reactive(),
                        reactiveMSSQLRuntimeConfig.getValue().dataSources().get(dataSourceName).reactive().mssql(),
                        tlsRegistrySupplier != null ? tlsRegistrySupplier.get() : null,
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
            DataSourceReactiveMSSQLConfig dataSourceReactiveMSSQLConfig,
            TlsConfigurationRegistry tlsRegistry,
            SyntheticCreationalContext<Pool> context) {
        PoolOptions poolOptions = ReactivePoolUtil.toPoolOptions(eventLoopCount, dataSourceReactiveRuntimeConfig);
        MSSQLConnectOptions mssqlConnectOptions = toMSSQLConnectOptions(dataSourceName, dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactiveMSSQLConfig, tlsRegistry);
        Supplier<Future<SqlConnectOptions>> databasesSupplier = ReactivePoolUtil.toDatabasesSupplier(
                List.of(mssqlConnectOptions), dataSourceRuntimeConfig, MSSQLConnectOptions::new);
        return createPool(vertx, poolOptions, mssqlConnectOptions, dataSourceName, databasesSupplier, context);
    }

    private MSSQLConnectOptions toMSSQLConnectOptions(String dataSourceName, DataSourceRuntimeConfig dataSourceRuntimeConfig,
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

        if (dataSourceReactiveMSSQLConfig.ssl()) {
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

    private Pool createPool(Vertx vertx, PoolOptions poolOptions, MSSQLConnectOptions mssqlConnectOptions,
            String dataSourceName, Supplier<Future<SqlConnectOptions>> databases,
            SyntheticCreationalContext<Pool> context) {
        Instance<PoolCreator> instance = context.getInjectedReference(POOL_CREATOR_TYPE_LITERAL, qualifier(dataSourceName));
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
