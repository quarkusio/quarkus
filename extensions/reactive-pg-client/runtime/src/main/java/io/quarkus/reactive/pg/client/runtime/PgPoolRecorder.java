package io.quarkus.reactive.pg.client.runtime;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxTrustOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;

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
import io.quarkus.reactive.pg.client.PgPoolCreator;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.pgclient.SslMode;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.impl.Utils;

@Recorder
public class PgPoolRecorder {

    public RuntimeValue<PgPool> configurePgPool(RuntimeValue<Vertx> vertx,
            Supplier<Integer> eventLoopCount,
            String dataSourceName,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesReactiveRuntimeConfig dataSourcesReactiveRuntimeConfig,
            DataSourcesReactivePostgreSQLConfig dataSourcesReactivePostgreSQLConfig,
            ShutdownContext shutdown) {

        PgPool pgPool = initialize((VertxInternal) vertx.getValue(),
                eventLoopCount.get(),
                dataSourceName,
                dataSourcesRuntimeConfig.dataSources().get(dataSourceName),
                dataSourcesReactiveRuntimeConfig.getDataSourceReactiveRuntimeConfig(dataSourceName),
                dataSourcesReactivePostgreSQLConfig.dataSources().get(dataSourceName).reactive().postgresql());

        shutdown.addShutdownTask(pgPool::close);
        return new RuntimeValue<>(pgPool);
    }

    public RuntimeValue<io.vertx.mutiny.pgclient.PgPool> mutinyPgPool(RuntimeValue<PgPool> pgPool) {
        return new RuntimeValue<>(io.vertx.mutiny.pgclient.PgPool.newInstance(pgPool.getValue()));
    }

    private PgPool initialize(VertxInternal vertx,
            Integer eventLoopCount,
            String dataSourceName,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig) {
        PoolOptions poolOptions = toPoolOptions(eventLoopCount, dataSourceRuntimeConfig, dataSourceReactiveRuntimeConfig,
                dataSourceReactivePostgreSQLConfig);
        List<PgConnectOptions> pgConnectOptionsList = toPgConnectOptions(dataSourceName, dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactivePostgreSQLConfig);
        Supplier<Future<PgConnectOptions>> databasesSupplier = toDatabasesSupplier(vertx, pgConnectOptionsList,
                dataSourceRuntimeConfig);
        return createPool(vertx, poolOptions, pgConnectOptionsList, dataSourceName, databasesSupplier);
    }

    private Supplier<Future<PgConnectOptions>> toDatabasesSupplier(Vertx vertx, List<PgConnectOptions> pgConnectOptionsList,
            DataSourceRuntimeConfig dataSourceRuntimeConfig) {
        Supplier<Future<PgConnectOptions>> supplier;
        if (dataSourceRuntimeConfig.credentialsProvider().isPresent()) {
            String beanName = dataSourceRuntimeConfig.credentialsProviderName().orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = dataSourceRuntimeConfig.credentialsProvider().get();
            supplier = new ConnectOptionsSupplier<>(vertx, credentialsProvider, name, pgConnectOptionsList,
                    PgConnectOptions::new);
        } else {
            supplier = Utils.roundRobinSupplier(pgConnectOptionsList);
        }
        return supplier;
    }

    private PoolOptions toPoolOptions(Integer eventLoopCount,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig) {
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

    private List<PgConnectOptions> toPgConnectOptions(String dataSourceName, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig) {
        List<PgConnectOptions> pgConnectOptionsList = new ArrayList<>();

        if (dataSourceReactiveRuntimeConfig.url().isPresent()) {
            List<String> urls = dataSourceReactiveRuntimeConfig.url().get();
            urls.forEach(url -> {
                // clean up the URL to make migrations easier
                if (url.matches("^vertx-reactive:postgre(?:s|sql)://.*$")) {
                    url = url.substring("vertx-reactive:".length());
                }
                pgConnectOptionsList.add(PgConnectOptions.fromUri(url));
            });
        } else {
            pgConnectOptionsList.add(new PgConnectOptions());
        }

        pgConnectOptionsList.forEach(pgConnectOptions -> {
            dataSourceRuntimeConfig.username().ifPresent(pgConnectOptions::setUser);

            dataSourceRuntimeConfig.password().ifPresent(pgConnectOptions::setPassword);

            // credentials provider
            if (dataSourceRuntimeConfig.credentialsProvider().isPresent()) {
                String beanName = dataSourceRuntimeConfig.credentialsProviderName().orElse(null);
                CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
                String name = dataSourceRuntimeConfig.credentialsProvider().get();
                Map<String, String> credentials = credentialsProvider.getCredentials(name);
                String user = credentials.get(USER_PROPERTY_NAME);
                String password = credentials.get(PASSWORD_PROPERTY_NAME);
                if (user != null) {
                    pgConnectOptions.setUser(user);
                }
                if (password != null) {
                    pgConnectOptions.setPassword(password);
                }
            }

            pgConnectOptions.setCachePreparedStatements(dataSourceReactiveRuntimeConfig.cachePreparedStatements());

            if (dataSourceReactivePostgreSQLConfig.pipeliningLimit().isPresent()) {
                pgConnectOptions.setPipeliningLimit(dataSourceReactivePostgreSQLConfig.pipeliningLimit().getAsInt());
            }

            if (dataSourceReactivePostgreSQLConfig.sslMode().isPresent()) {
                final SslMode sslMode = dataSourceReactivePostgreSQLConfig.sslMode().get();
                pgConnectOptions.setSslMode(sslMode);

                // If sslMode is verify-full, we also need a hostname verification algorithm
                if (sslMode == SslMode.VERIFY_FULL
                        && (!dataSourceReactiveRuntimeConfig.hostnameVerificationAlgorithm().isPresent()
                                || "".equals(dataSourceReactiveRuntimeConfig.hostnameVerificationAlgorithm().get()))) {
                    throw new IllegalArgumentException(
                            "quarkus.datasource.reactive.hostname-verification-algorithm must be specified under verify-full sslmode");
                }
            }

            pgConnectOptions.setTrustAll(dataSourceReactiveRuntimeConfig.trustAll());

            configurePemTrustOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificatePem());
            configureJksTrustOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificateJks());
            configurePfxTrustOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificatePfx());

            configurePemKeyCertOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificatePem());
            configureJksKeyCertOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificateJks());
            configurePfxKeyCertOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificatePfx());

            pgConnectOptions.setReconnectAttempts(dataSourceReactiveRuntimeConfig.reconnectAttempts());

            pgConnectOptions.setReconnectInterval(dataSourceReactiveRuntimeConfig.reconnectInterval().toMillis());

            dataSourceReactiveRuntimeConfig.hostnameVerificationAlgorithm().ifPresent(
                    pgConnectOptions::setHostnameVerificationAlgorithm);

            dataSourceReactiveRuntimeConfig.additionalProperties().forEach(pgConnectOptions::addProperty);

            // Use the convention defined by Quarkus Micrometer Vert.x metrics to create metrics prefixed with postgresql.
            // and the client_name as tag.
            // See io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractPrefix and
            // io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractClientName
            pgConnectOptions.setMetricsName("postgresql|" + dataSourceName);

        });

        return pgConnectOptionsList;
    }

    private PgPool createPool(Vertx vertx, PoolOptions poolOptions, List<PgConnectOptions> pgConnectOptionsList,
            String dataSourceName, Supplier<Future<PgConnectOptions>> databases) {
        Instance<PgPoolCreator> instance;
        if (DataSourceUtil.isDefault(dataSourceName)) {
            instance = Arc.container().select(PgPoolCreator.class);
        } else {
            instance = Arc.container().select(PgPoolCreator.class,
                    new ReactiveDataSource.ReactiveDataSourceLiteral(dataSourceName));
        }
        if (instance.isResolvable()) {
            PgPoolCreator.Input input = new DefaultInput(vertx, poolOptions, pgConnectOptionsList);
            return instance.get().create(input);
        }
        return PgPool.pool(vertx, databases, poolOptions);
    }

    private static class DefaultInput implements PgPoolCreator.Input {
        private final Vertx vertx;
        private final PoolOptions poolOptions;
        private final List<PgConnectOptions> pgConnectOptionsList;

        public DefaultInput(Vertx vertx, PoolOptions poolOptions, List<PgConnectOptions> pgConnectOptionsList) {
            this.vertx = vertx;
            this.poolOptions = poolOptions;
            this.pgConnectOptionsList = pgConnectOptionsList;
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
        public List<PgConnectOptions> pgConnectOptionsList() {
            return pgConnectOptionsList;
        }
    }
}
