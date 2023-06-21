package io.quarkus.reactive.oracle.client.runtime;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.reactive.datasource.runtime.ConnectOptionsSupplier;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveRuntimeConfig;
import io.quarkus.reactive.oracle.client.OraclePoolCreator;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.oracleclient.OracleConnectOptions;
import io.vertx.oracleclient.OraclePool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.impl.Utils;

@SuppressWarnings("deprecation")
@Recorder
public class OraclePoolRecorder {

    private static final Logger log = Logger.getLogger(OraclePoolRecorder.class);

    public RuntimeValue<OraclePool> configureOraclePool(RuntimeValue<Vertx> vertx,
            Supplier<Integer> eventLoopCount,
            String dataSourceName,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesReactiveRuntimeConfig dataSourcesReactiveRuntimeConfig,
            DataSourcesReactiveOracleConfig dataSourcesReactiveOracleConfig,
            ShutdownContext shutdown) {

        OraclePool oraclePool = initialize((VertxInternal) vertx.getValue(),
                eventLoopCount.get(),
                dataSourceName,
                dataSourcesRuntimeConfig.getDataSourceRuntimeConfig(dataSourceName),
                dataSourcesReactiveRuntimeConfig.getDataSourceReactiveRuntimeConfig(dataSourceName),
                dataSourcesReactiveOracleConfig.getDataSourceReactiveRuntimeConfig(dataSourceName));

        shutdown.addShutdownTask(oraclePool::close);
        return new RuntimeValue<>(oraclePool);
    }

    public RuntimeValue<io.vertx.mutiny.oracleclient.OraclePool> mutinyOraclePool(RuntimeValue<OraclePool> oraclePool) {
        return new RuntimeValue<>(io.vertx.mutiny.oracleclient.OraclePool.newInstance(oraclePool.getValue()));
    }

    private OraclePool initialize(VertxInternal vertx,
            Integer eventLoopCount,
            String dataSourceName,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveOracleConfig dataSourceReactiveOracleConfig) {
        PoolOptions poolOptions = toPoolOptions(eventLoopCount, dataSourceRuntimeConfig, dataSourceReactiveRuntimeConfig,
                dataSourceReactiveOracleConfig);
        OracleConnectOptions oracleConnectOptions = toOracleConnectOptions(dataSourceName, dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactiveOracleConfig);
        Supplier<Future<OracleConnectOptions>> databasesSupplier = toDatabasesSupplier(vertx, List.of(oracleConnectOptions),
                dataSourceRuntimeConfig);
        return createPool(vertx, poolOptions, oracleConnectOptions, dataSourceName, databasesSupplier);
    }

    private Supplier<Future<OracleConnectOptions>> toDatabasesSupplier(Vertx vertx,
            List<OracleConnectOptions> oracleConnectOptions,
            DataSourceRuntimeConfig dataSourceRuntimeConfig) {
        Supplier<Future<OracleConnectOptions>> supplier;
        if (dataSourceRuntimeConfig.credentialsProvider.isPresent()) {
            String beanName = dataSourceRuntimeConfig.credentialsProviderName.orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = dataSourceRuntimeConfig.credentialsProvider.get();
            supplier = new ConnectOptionsSupplier<>(vertx, credentialsProvider, name, oracleConnectOptions,
                    OracleConnectOptions::new);
        } else {
            supplier = Utils.roundRobinSupplier(oracleConnectOptions);
        }
        return supplier;
    }

    private PoolOptions toPoolOptions(Integer eventLoopCount,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveOracleConfig dataSourceReactiveOracleConfig) {
        PoolOptions poolOptions;
        poolOptions = new PoolOptions();

        poolOptions.setMaxSize(dataSourceReactiveRuntimeConfig.maxSize);

        if (dataSourceReactiveRuntimeConfig.idleTimeout.isPresent()) {
            int idleTimeout = Math.toIntExact(dataSourceReactiveRuntimeConfig.idleTimeout.get().toMillis());
            poolOptions.setIdleTimeout(idleTimeout).setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
        }

        if (dataSourceReactiveRuntimeConfig.shared) {
            poolOptions.setShared(true);
            if (dataSourceReactiveRuntimeConfig.name.isPresent()) {
                poolOptions.setName(dataSourceReactiveRuntimeConfig.name.get());
            }
        }

        if (dataSourceReactiveRuntimeConfig.eventLoopSize.isPresent()) {
            poolOptions.setEventLoopSize(Math.max(0, dataSourceReactiveRuntimeConfig.eventLoopSize.getAsInt()));
        } else if (eventLoopCount != null) {
            poolOptions.setEventLoopSize(Math.max(0, eventLoopCount));
        }

        return poolOptions;
    }

    private OracleConnectOptions toOracleConnectOptions(String dataSourceName, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveOracleConfig dataSourceReactiveOracleConfig) {
        OracleConnectOptions oracleConnectOptions;
        if (dataSourceReactiveRuntimeConfig.url.isPresent()) {
            List<String> urls = dataSourceReactiveRuntimeConfig.url.get();
            if (urls.size() > 1) {
                log.warn("The Reactive Oracle client does not support multiple URLs. The first one will be used, and " +
                        "others will be ignored.");
            }
            String url = urls.get(0);
            // clean up the URL to make migrations easier
            if (url.startsWith("vertx-reactive:oracle:")) {
                url = url.substring("vertx-reactive:".length());
            }
            oracleConnectOptions = OracleConnectOptions.fromUri(url);
        } else {
            oracleConnectOptions = new OracleConnectOptions();
        }

        if (dataSourceRuntimeConfig.username.isPresent()) {
            oracleConnectOptions.setUser(dataSourceRuntimeConfig.username.get());
        }

        if (dataSourceRuntimeConfig.password.isPresent()) {
            oracleConnectOptions.setPassword(dataSourceRuntimeConfig.password.get());
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
                oracleConnectOptions.setUser(user);
            }
            if (password != null) {
                oracleConnectOptions.setPassword(password);
            }
        }

        dataSourceReactiveRuntimeConfig.additionalProperties.forEach(oracleConnectOptions::addProperty);

        // Use the convention defined by Quarkus Micrometer Vert.x metrics to create metrics prefixed with oracle.
        // and the client_name as tag.
        // See io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractPrefix and
        // io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractClientName
        oracleConnectOptions.setMetricsName("oracle|" + dataSourceName);

        return oracleConnectOptions;
    }

    private OraclePool createPool(Vertx vertx, PoolOptions poolOptions, OracleConnectOptions oracleConnectOptions,
            String dataSourceName, Supplier<Future<OracleConnectOptions>> databases) {
        Instance<OraclePoolCreator> instance;
        if (DataSourceUtil.isDefault(dataSourceName)) {
            instance = Arc.container().select(OraclePoolCreator.class);
        } else {
            instance = Arc.container().select(OraclePoolCreator.class,
                    new ReactiveDataSource.ReactiveDataSourceLiteral(dataSourceName));
        }
        if (instance.isResolvable()) {
            OraclePoolCreator.Input input = new DefaultInput(vertx, poolOptions, oracleConnectOptions);
            return instance.get().create(input);
        }
        return OraclePool.pool(vertx, databases, poolOptions);
    }

    private static class DefaultInput implements OraclePoolCreator.Input {
        private final Vertx vertx;
        private final PoolOptions poolOptions;
        private final OracleConnectOptions oracleConnectOptions;

        public DefaultInput(Vertx vertx, PoolOptions poolOptions, OracleConnectOptions oracleConnectOptions) {
            this.vertx = vertx;
            this.poolOptions = poolOptions;
            this.oracleConnectOptions = oracleConnectOptions;
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
        public OracleConnectOptions oracleConnectOptions() {
            return oracleConnectOptions;
        }
    }
}
