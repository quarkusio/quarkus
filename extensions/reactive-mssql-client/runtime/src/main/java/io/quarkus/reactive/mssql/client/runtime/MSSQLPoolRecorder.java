package io.quarkus.reactive.mssql.client.runtime;

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
import io.quarkus.reactive.mssql.client.MSSQLPoolCreator;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.impl.Utils;

@Recorder
public class MSSQLPoolRecorder {

    private static final Logger log = Logger.getLogger(MSSQLPoolRecorder.class);

    public RuntimeValue<MSSQLPool> configureMSSQLPool(RuntimeValue<Vertx> vertx,
            Supplier<Integer> eventLoopCount,
            String dataSourceName,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesReactiveRuntimeConfig dataSourcesReactiveRuntimeConfig,
            DataSourcesReactiveMSSQLConfig dataSourcesReactiveMSSQLConfig,
            ShutdownContext shutdown) {

        MSSQLPool mssqlPool = initialize((VertxInternal) vertx.getValue(),
                eventLoopCount.get(),
                dataSourceName,
                dataSourcesRuntimeConfig.dataSources().get(dataSourceName),
                dataSourcesReactiveRuntimeConfig.getDataSourceReactiveRuntimeConfig(dataSourceName),
                dataSourcesReactiveMSSQLConfig.dataSources().get(dataSourceName).reactive().mssql());

        shutdown.addShutdownTask(mssqlPool::close);
        return new RuntimeValue<>(mssqlPool);
    }

    public RuntimeValue<io.vertx.mutiny.mssqlclient.MSSQLPool> mutinyMSSQLPool(RuntimeValue<MSSQLPool> mssqlPool) {
        return new RuntimeValue<>(io.vertx.mutiny.mssqlclient.MSSQLPool.newInstance(mssqlPool.getValue()));
    }

    private MSSQLPool initialize(VertxInternal vertx,
            Integer eventLoopCount,
            String dataSourceName, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMSSQLConfig dataSourceReactiveMSSQLConfig) {
        PoolOptions poolOptions = toPoolOptions(eventLoopCount, dataSourceRuntimeConfig, dataSourceReactiveRuntimeConfig,
                dataSourceReactiveMSSQLConfig);
        MSSQLConnectOptions mssqlConnectOptions = toMSSQLConnectOptions(dataSourceName, dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactiveMSSQLConfig);
        Supplier<Future<MSSQLConnectOptions>> databasesSupplier = toDatabasesSupplier(vertx, List.of(mssqlConnectOptions),
                dataSourceRuntimeConfig);
        return createPool(vertx, poolOptions, mssqlConnectOptions, dataSourceName, databasesSupplier);
    }

    private Supplier<Future<MSSQLConnectOptions>> toDatabasesSupplier(Vertx vertx,
            List<MSSQLConnectOptions> mssqlConnectOptionsList,
            DataSourceRuntimeConfig dataSourceRuntimeConfig) {
        Supplier<Future<MSSQLConnectOptions>> supplier;
        if (dataSourceRuntimeConfig.credentialsProvider().isPresent()) {
            String beanName = dataSourceRuntimeConfig.credentialsProviderName().orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = dataSourceRuntimeConfig.credentialsProvider().get();
            supplier = new ConnectOptionsSupplier<>(vertx, credentialsProvider, name, mssqlConnectOptionsList,
                    MSSQLConnectOptions::new);
        } else {
            supplier = Utils.roundRobinSupplier(mssqlConnectOptionsList);
        }
        return supplier;
    }

    private PoolOptions toPoolOptions(Integer eventLoopCount,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMSSQLConfig dataSourceReactiveMSSQLConfig) {
        PoolOptions poolOptions;
        poolOptions = new PoolOptions();

        poolOptions.setMaxSize(dataSourceReactiveRuntimeConfig.maxSize());

        if (dataSourceReactiveRuntimeConfig.idleTimeout().isPresent()) {
            int idleTimeout = Math.toIntExact(dataSourceReactiveRuntimeConfig.idleTimeout().get().toMillis());
            poolOptions.setIdleTimeout(idleTimeout).setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
        }

        if (dataSourceReactiveRuntimeConfig.shared()) {
            poolOptions.setShared(true);
            if (dataSourceReactiveRuntimeConfig.name().isPresent()) {
                poolOptions.setName(dataSourceReactiveRuntimeConfig.name().get());
            }
        }

        if (dataSourceReactiveRuntimeConfig.eventLoopSize().isPresent()) {
            poolOptions.setEventLoopSize(Math.max(0, dataSourceReactiveRuntimeConfig.eventLoopSize().getAsInt()));
        } else if (eventLoopCount != null) {
            poolOptions.setEventLoopSize(Math.max(0, eventLoopCount));
        }

        return poolOptions;
    }

    private MSSQLConnectOptions toMSSQLConnectOptions(String dataSourceName, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMSSQLConfig dataSourceReactiveMSSQLConfig) {
        MSSQLConnectOptions mssqlConnectOptions;
        if (dataSourceReactiveRuntimeConfig.url().isPresent()) {
            List<String> urls = dataSourceReactiveRuntimeConfig.url().get();
            if (urls.size() > 1) {
                log.warn("The Reactive MSSQL client does not support multiple URLs. The first one will be used, and " +
                        "others will be ignored.");
            }
            String url = urls.get(0);
            // clean up the URL to make migrations easier
            if (url.startsWith("vertx-reactive:sqlserver://")) {
                url = url.substring("vertx-reactive:".length());
            }
            mssqlConnectOptions = MSSQLConnectOptions.fromUri(url);
        } else {
            mssqlConnectOptions = new MSSQLConnectOptions();
        }

        if (dataSourceReactiveMSSQLConfig.packetSize().isPresent()) {
            mssqlConnectOptions.setPacketSize(dataSourceReactiveMSSQLConfig.packetSize().getAsInt());
        }

        if (dataSourceRuntimeConfig.username().isPresent()) {
            mssqlConnectOptions.setUser(dataSourceRuntimeConfig.username().get());
        }

        if (dataSourceRuntimeConfig.password().isPresent()) {
            mssqlConnectOptions.setPassword(dataSourceRuntimeConfig.password().get());
        }

        // credentials provider
        if (dataSourceRuntimeConfig.credentialsProvider().isPresent()) {
            String beanName = dataSourceRuntimeConfig.credentialsProviderName().orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = dataSourceRuntimeConfig.credentialsProvider().get();
            Map<String, String> credentials = credentialsProvider.getCredentials(name);
            String user = credentials.get(USER_PROPERTY_NAME);
            String password = credentials.get(PASSWORD_PROPERTY_NAME);
            if (user != null) {
                mssqlConnectOptions.setUser(user);
            }
            if (password != null) {
                mssqlConnectOptions.setPassword(password);
            }
        }

        mssqlConnectOptions.setReconnectAttempts(dataSourceReactiveRuntimeConfig.reconnectAttempts());

        mssqlConnectOptions.setReconnectInterval(dataSourceReactiveRuntimeConfig.reconnectInterval().toMillis());

        mssqlConnectOptions.setSsl(dataSourceReactiveMSSQLConfig.ssl());

        mssqlConnectOptions.setTrustAll(dataSourceReactiveRuntimeConfig.trustAll());

        configurePemTrustOptions(mssqlConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificatePem());
        configureJksTrustOptions(mssqlConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificateJks());
        configurePfxTrustOptions(mssqlConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificatePfx());

        configurePemKeyCertOptions(mssqlConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificatePem());
        configureJksKeyCertOptions(mssqlConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificateJks());
        configurePfxKeyCertOptions(mssqlConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificatePfx());

        if (dataSourceReactiveRuntimeConfig.hostnameVerificationAlgorithm().isPresent()) {
            mssqlConnectOptions.setHostnameVerificationAlgorithm(
                    dataSourceReactiveRuntimeConfig.hostnameVerificationAlgorithm().get());
        }

        dataSourceReactiveRuntimeConfig.additionalProperties().forEach(mssqlConnectOptions::addProperty);

        // Use the convention defined by Quarkus Micrometer Vert.x metrics to create metrics prefixed with mssql.
        // with the client_name as tag.
        // See io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractPrefix and
        // io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractClientName
        mssqlConnectOptions.setMetricsName("mssql|" + dataSourceName);

        return mssqlConnectOptions;
    }

    private MSSQLPool createPool(Vertx vertx, PoolOptions poolOptions, MSSQLConnectOptions mSSQLConnectOptions,
            String dataSourceName, Supplier<Future<MSSQLConnectOptions>> databases) {
        Instance<MSSQLPoolCreator> instance;
        if (DataSourceUtil.isDefault(dataSourceName)) {
            instance = Arc.container().select(MSSQLPoolCreator.class);
        } else {
            instance = Arc.container().select(MSSQLPoolCreator.class,
                    new ReactiveDataSource.ReactiveDataSourceLiteral(dataSourceName));
        }
        if (instance.isResolvable()) {
            MSSQLPoolCreator.Input input = new DefaultInput(vertx, poolOptions, mSSQLConnectOptions);
            return instance.get().create(input);
        }
        return MSSQLPool.pool(vertx, databases, poolOptions);
    }

    private static class DefaultInput implements MSSQLPoolCreator.Input {
        private final Vertx vertx;
        private final PoolOptions poolOptions;
        private final MSSQLConnectOptions mSSQLConnectOptions;

        public DefaultInput(Vertx vertx, PoolOptions poolOptions, MSSQLConnectOptions mSSQLConnectOptions) {
            this.vertx = vertx;
            this.poolOptions = poolOptions;
            this.mSSQLConnectOptions = mSSQLConnectOptions;
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
        public MSSQLConnectOptions msSQLConnectOptions() {
            return mSSQLConnectOptions;
        }
    }
}
