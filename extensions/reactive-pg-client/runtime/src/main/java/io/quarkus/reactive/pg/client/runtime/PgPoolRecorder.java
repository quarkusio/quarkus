package io.quarkus.reactive.pg.client.runtime;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;
import static io.quarkus.reactive.datasource.runtime.ReactiveDataSourceUtil.qualifier;
import static io.quarkus.reactive.datasource.runtime.UnitisedTime.unitised;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxTrustOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
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
import io.vertx.pgclient.spi.PgDriver;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.impl.Utils;

@Recorder
public class PgPoolRecorder {

    private static final TypeLiteral<Instance<PgPoolCreator>> PG_POOL_CREATOR_TYPE_LITERAL = new TypeLiteral<>() {
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

    public Supplier<ActiveResult> poolCheckActiveSupplier(String dataSourceName) {
        return new Supplier<>() {
            @Override
            public ActiveResult get() {
                Optional<Boolean> active = runtimeConfig.getValue().dataSources().get(dataSourceName).active();
                if (active.isPresent() && !active.get()) {
                    return ActiveResult.inactive(DataSourceUtil.dataSourceInactiveReasonDeactivated(dataSourceName));
                }
                if (reactiveRuntimeConfig.getValue().dataSources().get(dataSourceName).reactive().url().isEmpty()) {
                    return ActiveResult.inactive(DataSourceUtil.dataSourceInactiveReasonUrlMissing(dataSourceName,
                            "reactive.url"));
                }
                return ActiveResult.active();
            }
        };
    }

    public Function<SyntheticCreationalContext<PgPool>, PgPool> configurePgPool(RuntimeValue<Vertx> vertx,
            Supplier<Integer> eventLoopCount, String dataSourceName, ShutdownContext shutdown) {
        return new Function<>() {
            @Override
            public PgPool apply(SyntheticCreationalContext<PgPool> context) {
                PgPool pgPool = initialize((VertxInternal) vertx.getValue(),
                        eventLoopCount.get(),
                        dataSourceName,
                        runtimeConfig.getValue().dataSources().get(dataSourceName),
                        reactiveRuntimeConfig.getValue().dataSources().get(dataSourceName).reactive(),
                        reactivePostgreRuntimeConfig.getValue().dataSources().get(dataSourceName).reactive().postgresql(),
                        context);

                shutdown.addShutdownTask(pgPool::close);
                return pgPool;
            }
        };
    }

    public Function<SyntheticCreationalContext<io.vertx.mutiny.pgclient.PgPool>, io.vertx.mutiny.pgclient.PgPool> mutinyPgPool(
            String dataSourceName) {
        return new Function<>() {
            @SuppressWarnings("unchecked")
            @Override
            public io.vertx.mutiny.pgclient.PgPool apply(SyntheticCreationalContext context) {
                return io.vertx.mutiny.pgclient.PgPool.newInstance(
                        (PgPool) context.getInjectedReference(PgPool.class, qualifier(dataSourceName)));
            }
        };
    }

    private PgPool initialize(VertxInternal vertx,
            Integer eventLoopCount,
            String dataSourceName,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig,
            SyntheticCreationalContext<PgPool> context) {
        PoolOptions poolOptions = toPoolOptions(eventLoopCount, dataSourceReactiveRuntimeConfig);
        List<PgConnectOptions> pgConnectOptionsList = toPgConnectOptions(dataSourceName, dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactivePostgreSQLConfig);
        Supplier<Future<PgConnectOptions>> databasesSupplier = toDatabasesSupplier(vertx, pgConnectOptionsList,
                dataSourceRuntimeConfig);
        return createPool(vertx, poolOptions, pgConnectOptionsList, dataSourceName, databasesSupplier, context);
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
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig) {
        PoolOptions poolOptions;
        poolOptions = new PoolOptions();

        poolOptions.setMaxSize(dataSourceReactiveRuntimeConfig.maxSize());

        if (dataSourceReactiveRuntimeConfig.idleTimeout().isPresent()) {
            var idleTimeout = unitised(dataSourceReactiveRuntimeConfig.idleTimeout().get());
            poolOptions.setIdleTimeout(idleTimeout.value).setIdleTimeoutUnit(idleTimeout.unit);
        }

        if (dataSourceReactiveRuntimeConfig.maxLifetime().isPresent()) {
            var maxLifetime = unitised(dataSourceReactiveRuntimeConfig.maxLifetime().get());
            poolOptions.setMaxLifetime(maxLifetime.value).setMaxLifetimeUnit(maxLifetime.unit);
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

                var algo = dataSourceReactiveRuntimeConfig.hostnameVerificationAlgorithm();
                // If sslMode is verify-full, we also need a hostname verification algorithm
                if ("NONE".equalsIgnoreCase(algo) && sslMode == SslMode.VERIFY_FULL) {
                    throw new IllegalArgumentException(
                            "quarkus.datasource.reactive.hostname-verification-algorithm must be specified under verify-full sslmode");
                }
            }

            pgConnectOptions.setUseLayer7Proxy(dataSourceReactivePostgreSQLConfig.useLayer7Proxy());

            pgConnectOptions.setTrustAll(dataSourceReactiveRuntimeConfig.trustAll());

            configurePemTrustOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificatePem());
            configureJksTrustOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificateJks());
            configurePfxTrustOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificatePfx());

            configurePemKeyCertOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificatePem());
            configureJksKeyCertOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificateJks());
            configurePfxKeyCertOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificatePfx());

            pgConnectOptions.setReconnectAttempts(dataSourceReactiveRuntimeConfig.reconnectAttempts());

            pgConnectOptions.setReconnectInterval(dataSourceReactiveRuntimeConfig.reconnectInterval().toMillis());

            var algo = dataSourceReactiveRuntimeConfig.hostnameVerificationAlgorithm();
            if ("NONE".equalsIgnoreCase(algo)) {
                pgConnectOptions.setHostnameVerificationAlgorithm("");
            } else {
                pgConnectOptions.setHostnameVerificationAlgorithm(algo);
            }

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
            String dataSourceName, Supplier<Future<PgConnectOptions>> databases,
            SyntheticCreationalContext<PgPool> context) {
        Instance<PgPoolCreator> instance = context.getInjectedReference(PG_POOL_CREATOR_TYPE_LITERAL,
                qualifier(dataSourceName));
        if (instance.isResolvable()) {
            PgPoolCreator.Input input = new DefaultInput(vertx, poolOptions, pgConnectOptionsList);
            return (PgPool) instance.get().create(input);
        }
        return (PgPool) PgDriver.INSTANCE.createPool(vertx, databases, poolOptions);
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

    public RuntimeValue<PgPoolSupport> createPgPoolSupport(Set<String> pgPoolNames) {
        return new RuntimeValue<>(new PgPoolSupport(pgPoolNames));
    }
}
