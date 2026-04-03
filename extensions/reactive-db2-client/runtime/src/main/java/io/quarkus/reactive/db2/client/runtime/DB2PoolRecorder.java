package io.quarkus.reactive.db2.client.runtime;

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
import io.quarkus.reactive.db2.client.DB2PoolCreator;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.db2client.DB2Builder;
import io.vertx.db2client.DB2ConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;

@Recorder
public class DB2PoolRecorder {

    private static final boolean SUPPORTS_CACHE_PREPARED_STATEMENTS = true;

    private static final Logger log = Logger.getLogger(DB2PoolRecorder.class);
    private static final TypeLiteral<Instance<PoolCreator>> POOL_CREATOR_TYPE_LITERAL = new TypeLiteral<>() {
    };

    private final RuntimeValue<DataSourcesRuntimeConfig> runtimeConfig;
    private final RuntimeValue<DataSourcesReactiveRuntimeConfig> reactiveRuntimeConfig;
    private final RuntimeValue<DataSourcesReactiveDB2Config> reactiveDB2RuntimeConfig;

    public DB2PoolRecorder(
            RuntimeValue<DataSourcesRuntimeConfig> runtimeConfig,
            RuntimeValue<DataSourcesReactiveRuntimeConfig> reactiveRuntimeConfig,
            RuntimeValue<DataSourcesReactiveDB2Config> reactiveDB2RuntimeConfig) {
        this.runtimeConfig = runtimeConfig;
        this.reactiveRuntimeConfig = reactiveRuntimeConfig;
        this.reactiveDB2RuntimeConfig = reactiveDB2RuntimeConfig;
    }

    public Function<SyntheticCreationalContext<Pool>, Pool> configureDB2Pool(RuntimeValue<Vertx> vertx,
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
                        reactiveDB2RuntimeConfig.getValue().dataSources().get(dataSourceName).reactive().db2(),
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
            DataSourceReactiveDB2Config dataSourceReactiveDB2Config,
            TlsConfigurationRegistry tlsRegistry,
            SyntheticCreationalContext<Pool> context) {
        PoolOptions poolOptions = ReactivePoolUtil.toPoolOptions(eventLoopCount, dataSourceReactiveRuntimeConfig);
        DB2ConnectOptions db2ConnectOptions = toConnectOptions(dataSourceName, dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactiveDB2Config, tlsRegistry);
        Supplier<Future<SqlConnectOptions>> databasesSupplier = ReactivePoolUtil.toDatabasesSupplier(
                List.of(db2ConnectOptions), dataSourceRuntimeConfig, DB2ConnectOptions::new);
        return createPool(vertx, poolOptions, db2ConnectOptions, dataSourceName, databasesSupplier, context);
    }

    private DB2ConnectOptions toConnectOptions(String dataSourceName, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveDB2Config dataSourceReactiveDB2Config,
            TlsConfigurationRegistry tlsRegistry) {
        DB2ConnectOptions connectOptions;

        if (dataSourceReactiveRuntimeConfig.url().isPresent()) {
            List<String> urls = dataSourceReactiveRuntimeConfig.url().get();
            if (urls.size() > 1) {
                log.warn("The Reactive DB2 client does not support multiple URLs. The first one will be used, and " +
                        "others will be ignored.");
            }
            String url = urls.get(0);
            // clean up the URL to make migrations easier
            if (url.startsWith("vertx-reactive:")) {
                url = url.substring("vertx-reactive:".length());
            }
            connectOptions = DB2ConnectOptions.fromUri(url);
        } else {
            connectOptions = new DB2ConnectOptions();
        }

        ReactivePoolUtil.configureCredentials(connectOptions, dataSourceRuntimeConfig);

        connectOptions.setCachePreparedStatements(
                dataSourceReactiveRuntimeConfig.cachePreparedStatements().orElse(SUPPORTS_CACHE_PREPARED_STATEMENTS));

        if (dataSourceReactiveDB2Config.ssl()) {
            connectOptions.setSsl(true);
        } else if (dataSourceReactiveRuntimeConfig.tlsConfigurationName().isPresent()) {
            // Auto-enable SSL when a named TLS configuration is set
            connectOptions.setSsl(true);
        }

        ReactivePoolUtil.configureSsl(connectOptions, dataSourceReactiveRuntimeConfig, tlsRegistry);

        dataSourceReactiveRuntimeConfig.additionalProperties().forEach(connectOptions::addProperty);

        // Use the convention defined by Quarkus Micrometer Vert.x metrics to create metrics prefixed with db2.
        // and the client_name as tag.
        connectOptions.setMetricsName("db2|" + dataSourceName);

        return connectOptions;
    }

    private Pool createPool(Vertx vertx, PoolOptions poolOptions, DB2ConnectOptions db2ConnectOptions,
            String dataSourceName, Supplier<Future<SqlConnectOptions>> databases,
            SyntheticCreationalContext<Pool> context) {
        Instance<PoolCreator> instance = context.getInjectedReference(POOL_CREATOR_TYPE_LITERAL, qualifier(dataSourceName));
        if (instance.isResolvable()) {
            DB2PoolCreator.Input input = new DefaultInput(vertx, poolOptions, db2ConnectOptions);
            return instance.get().create(input);
        }
        return DB2Builder.pool().with(poolOptions)
                .connectingTo(databases)
                .using(vertx).build();
    }

    private static class DefaultInput implements DB2PoolCreator.Input {
        private final Vertx vertx;
        private final PoolOptions poolOptions;
        private final DB2ConnectOptions connectOptions;

        public DefaultInput(Vertx vertx, PoolOptions poolOptions, DB2ConnectOptions connectOptions) {
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
        public DB2ConnectOptions db2ConnectOptions() {
            return connectOptions;
        }
    }
}
