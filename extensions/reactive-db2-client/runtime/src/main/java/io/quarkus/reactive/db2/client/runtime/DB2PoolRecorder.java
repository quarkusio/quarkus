package io.quarkus.reactive.db2.client.runtime;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxTrustOptions;

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
import io.quarkus.reactive.db2.client.DB2PoolCreator;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.db2client.DB2ConnectOptions;
import io.vertx.db2client.DB2Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.impl.Utils;

@Recorder
public class DB2PoolRecorder {

    private static final Logger log = Logger.getLogger(DB2PoolRecorder.class);

    public RuntimeValue<DB2Pool> configureDB2Pool(RuntimeValue<Vertx> vertx,
            Supplier<Integer> eventLoopCount,
            String dataSourceName,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesReactiveRuntimeConfig dataSourcesReactiveRuntimeConfig,
            DataSourcesReactiveDB2Config dataSourcesReactiveDB2Config,
            ShutdownContext shutdown) {

        DB2Pool db2Pool = initialize((VertxInternal) vertx.getValue(),
                eventLoopCount.get(),
                dataSourceName,
                dataSourcesRuntimeConfig.getDataSourceRuntimeConfig(dataSourceName),
                dataSourcesReactiveRuntimeConfig.getDataSourceReactiveRuntimeConfig(dataSourceName),
                dataSourcesReactiveDB2Config.getDataSourceReactiveRuntimeConfig(dataSourceName));

        shutdown.addShutdownTask(db2Pool::close);
        return new RuntimeValue<>(db2Pool);
    }

    public RuntimeValue<io.vertx.mutiny.db2client.DB2Pool> mutinyDB2Pool(RuntimeValue<DB2Pool> db2Pool) {
        return new RuntimeValue<>(io.vertx.mutiny.db2client.DB2Pool.newInstance(db2Pool.getValue()));
    }

    private DB2Pool initialize(VertxInternal vertx,
            Integer eventLoopCount,
            String dataSourceName,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveDB2Config dataSourceReactiveDB2Config) {
        PoolOptions poolOptions = toPoolOptions(eventLoopCount, dataSourceRuntimeConfig, dataSourceReactiveRuntimeConfig,
                dataSourceReactiveDB2Config);
        DB2ConnectOptions db2ConnectOptions = toConnectOptions(dataSourceName, dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactiveDB2Config);
        Supplier<Future<DB2ConnectOptions>> databasesSupplier = toDatabasesSupplier(vertx, List.of(db2ConnectOptions),
                dataSourceRuntimeConfig);
        return createPool(vertx, poolOptions, db2ConnectOptions, dataSourceName, databasesSupplier);
    }

    private Supplier<Future<DB2ConnectOptions>> toDatabasesSupplier(Vertx vertx, List<DB2ConnectOptions> db2ConnectOptionsList,
            DataSourceRuntimeConfig dataSourceRuntimeConfig) {
        Supplier<Future<DB2ConnectOptions>> supplier;
        if (dataSourceRuntimeConfig.credentialsProvider.isPresent()) {
            String beanName = dataSourceRuntimeConfig.credentialsProviderName.orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = dataSourceRuntimeConfig.credentialsProvider.get();
            supplier = new ConnectOptionsSupplier<>(vertx, credentialsProvider, name, db2ConnectOptionsList,
                    DB2ConnectOptions::new);
        } else {
            supplier = Utils.roundRobinSupplier(db2ConnectOptionsList);
        }
        return supplier;
    }

    private PoolOptions toPoolOptions(Integer eventLoopCount,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveDB2Config dataSourceReactiveDB2Config) {
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

    private DB2ConnectOptions toConnectOptions(String dataSourceName, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveDB2Config dataSourceReactiveDB2Config) {
        DB2ConnectOptions connectOptions;

        if (dataSourceReactiveRuntimeConfig.url.isPresent()) {
            List<String> urls = dataSourceReactiveRuntimeConfig.url.get();
            if (urls.size() > 1) {
                log.warn("The Reactive DB2 client does not support multiple URLs. The first one will be used, and " +
                        "others will be ignored.");
            }
            String url = urls.get(0);
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
                connectOptions.setPassword(password);
            }
        }

        connectOptions.setCachePreparedStatements(dataSourceReactiveRuntimeConfig.cachePreparedStatements);

        connectOptions.setSsl(dataSourceReactiveDB2Config.ssl);

        connectOptions.setTrustAll(dataSourceReactiveRuntimeConfig.trustAll);

        configurePemTrustOptions(connectOptions, dataSourceReactiveRuntimeConfig.trustCertificatePem);
        configureJksTrustOptions(connectOptions, dataSourceReactiveRuntimeConfig.trustCertificateJks);
        configurePfxTrustOptions(connectOptions, dataSourceReactiveRuntimeConfig.trustCertificatePfx);

        configurePemKeyCertOptions(connectOptions, dataSourceReactiveRuntimeConfig.keyCertificatePem);
        configureJksKeyCertOptions(connectOptions, dataSourceReactiveRuntimeConfig.keyCertificateJks);
        configurePfxKeyCertOptions(connectOptions, dataSourceReactiveRuntimeConfig.keyCertificatePfx);

        connectOptions.setReconnectAttempts(dataSourceReactiveRuntimeConfig.reconnectAttempts);

        connectOptions.setReconnectInterval(dataSourceReactiveRuntimeConfig.reconnectInterval.toMillis());

        if (dataSourceReactiveRuntimeConfig.hostnameVerificationAlgorithm.isPresent()) {
            connectOptions.setHostnameVerificationAlgorithm(
                    dataSourceReactiveRuntimeConfig.hostnameVerificationAlgorithm.get());
        }

        dataSourceReactiveRuntimeConfig.additionalProperties.forEach(connectOptions::addProperty);

        // Use the convention defined by Quarkus Micrometer Vert.x metrics to create metrics prefixed with db2.
        // and the client_name as tag.
        // See io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractPrefix and
        // io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractClientName
        connectOptions.setMetricsName("db2|" + dataSourceName);

        return connectOptions;
    }

    private DB2Pool createPool(Vertx vertx, PoolOptions poolOptions, DB2ConnectOptions dB2ConnectOptions,
            String dataSourceName, Supplier<Future<DB2ConnectOptions>> databases) {
        Instance<DB2PoolCreator> instance;
        if (DataSourceUtil.isDefault(dataSourceName)) {
            instance = Arc.container().select(DB2PoolCreator.class);
        } else {
            instance = Arc.container().select(DB2PoolCreator.class,
                    new ReactiveDataSource.ReactiveDataSourceLiteral(dataSourceName));
        }
        if (instance.isResolvable()) {
            DB2PoolCreator.Input input = new DefaultInput(vertx, poolOptions, dB2ConnectOptions);
            return instance.get().create(input);
        }
        return DB2Pool.pool(vertx, databases, poolOptions);
    }

    private static class DefaultInput implements DB2PoolCreator.Input {
        private final Vertx vertx;
        private final PoolOptions poolOptions;
        private final DB2ConnectOptions dB2ConnectOptions;

        public DefaultInput(Vertx vertx, PoolOptions poolOptions, DB2ConnectOptions dB2ConnectOptions) {
            this.vertx = vertx;
            this.poolOptions = poolOptions;
            this.dB2ConnectOptions = dB2ConnectOptions;
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
        public DB2ConnectOptions db2ConnectOptions() {
            return dB2ConnectOptions;
        }
    }
}
