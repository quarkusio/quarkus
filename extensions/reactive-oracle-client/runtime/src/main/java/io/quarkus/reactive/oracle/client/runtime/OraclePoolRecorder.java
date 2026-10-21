package io.quarkus.reactive.oracle.client.runtime;

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
import io.quarkus.reactive.oracle.client.OraclePoolCreator;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.oracleclient.OracleBuilder;
import io.vertx.oracleclient.OracleConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;

public final class OraclePoolRecorder {

    private static final Logger log = Logger.getLogger(OraclePoolRecorder.class);

    private OraclePoolRecorder() {
    }

    public static Optional<Pool> createPool(String dataSourceName, DataSourcesRuntimeConfig runtimeConfig,
            DataSourcesReactiveRuntimeConfig reactiveRuntimeConfig, Vertx vertx, int eventLoopCount,
            TlsConfigurationRegistry tlsRegistry) {
        if (!ReactivePoolUtil.checkActive(dataSourceName, runtimeConfig, reactiveRuntimeConfig).value()) {
            return Optional.empty();
        }
        return Optional.of(initialize((VertxInternal) vertx,
                eventLoopCount,
                dataSourceName,
                runtimeConfig.dataSources().get(dataSourceName),
                reactiveRuntimeConfig.dataSources().get(dataSourceName).reactive(),
                tlsRegistry));
    }

    private static Pool initialize(VertxInternal vertx,
            Integer eventLoopCount,
            String dataSourceName,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            TlsConfigurationRegistry tlsRegistry) {
        PoolOptions poolOptions = ReactivePoolUtil.toPoolOptions(eventLoopCount, dataSourceReactiveRuntimeConfig);
        OracleConnectOptions oracleConnectOptions = toOracleConnectOptions(dataSourceName, dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, tlsRegistry);
        Supplier<Future<SqlConnectOptions>> databasesSupplier = ReactivePoolUtil.toDatabasesSupplier(
                List.of(oracleConnectOptions), dataSourceRuntimeConfig, OracleConnectOptions::new);
        return createPool(vertx, poolOptions, oracleConnectOptions, dataSourceName, databasesSupplier);
    }

    private static OracleConnectOptions toOracleConnectOptions(String dataSourceName,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            TlsConfigurationRegistry tlsRegistry) {
        OracleConnectOptions oracleConnectOptions;
        if (dataSourceReactiveRuntimeConfig.url().isPresent()) {
            List<String> urls = dataSourceReactiveRuntimeConfig.url().get();
            if (urls.size() > 1) {
                log.warn("The Reactive Oracle client does not support multiple URLs. The first one will be used, and " +
                        "others will be ignored.");
            }
            String url = urls.get(0);
            // clean up the URL to make migrations easier
            if (url.startsWith("vertx-reactive:")) {
                url = url.substring("vertx-reactive:".length());
            }
            oracleConnectOptions = OracleConnectOptions.fromUri(url);
        } else {
            oracleConnectOptions = new OracleConnectOptions();
        }

        ReactivePoolUtil.configureCredentials(oracleConnectOptions, dataSourceRuntimeConfig);

        // Oracle has no SSL mode concept — TLS config is applied directly via ClientSSLOptions
        ReactivePoolUtil.configureSsl(oracleConnectOptions, dataSourceReactiveRuntimeConfig, tlsRegistry);

        dataSourceReactiveRuntimeConfig.additionalProperties().forEach(oracleConnectOptions::addProperty);

        // Use the convention defined by Quarkus Micrometer Vert.x metrics to create metrics prefixed with oracle.
        // and the client_name as tag.
        oracleConnectOptions.setMetricsName("oracle|" + dataSourceName);

        return oracleConnectOptions;
    }

    private static Pool createPool(Vertx vertx, PoolOptions poolOptions, OracleConnectOptions oracleConnectOptions,
            String dataSourceName, Supplier<Future<SqlConnectOptions>> databases) {
        Instance<PoolCreator> instance = Arc.container().select(PoolCreator.class, qualifier(dataSourceName));
        if (instance.isResolvable()) {
            OraclePoolCreator.Input input = new DefaultInput(vertx, poolOptions, oracleConnectOptions);
            return instance.get().create(input);
        }

        return OracleBuilder.pool().with(poolOptions)
                .connectingTo(databases)
                .using(vertx).build();
    }

    private static class DefaultInput implements OraclePoolCreator.Input {
        private final Vertx vertx;
        private final PoolOptions poolOptions;
        private final OracleConnectOptions connectOptions;

        public DefaultInput(Vertx vertx, PoolOptions poolOptions, OracleConnectOptions connectOptions) {
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
        public OracleConnectOptions oracleConnectOptions() {
            return connectOptions;
        }
    }
}
