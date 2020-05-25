package io.quarkus.reactive.pg.client.runtime;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxTrustOptions;

import java.util.Map;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.datasource.runtime.LegacyDataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.LegacyDataSourcesRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

@Recorder
@SuppressWarnings("deprecation")
public class PgPoolRecorder {

    public RuntimeValue<PgPool> configurePgPool(RuntimeValue<Vertx> vertx, BeanContainer container,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig,
            LegacyDataSourcesRuntimeConfig legacyDataSourcesRuntimeConfig,
            LegacyDataSourceReactivePostgreSQLConfig legacyDataSourceReactivePostgreSQLConfig,
            boolean isLegacy,
            ShutdownContext shutdown) {

        PgPool pgPool;
        if (!isLegacy) {
            pgPool = initialize(vertx.getValue(), dataSourcesRuntimeConfig.defaultDataSource,
                    dataSourceReactiveRuntimeConfig,
                    dataSourceReactivePostgreSQLConfig);
        } else {
            pgPool = legacyInitialize(vertx.getValue(), dataSourcesRuntimeConfig.defaultDataSource,
                    legacyDataSourcesRuntimeConfig.defaultDataSource, legacyDataSourceReactivePostgreSQLConfig);
        }

        PgPoolProducer producer = container.instance(PgPoolProducer.class);
        producer.initialize(pgPool);

        shutdown.addShutdownTask(pgPool::close);
        return new RuntimeValue<>(pgPool);
    }

    private PgPool initialize(Vertx vertx, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig) {
        PoolOptions poolOptions = toPoolOptions(dataSourceRuntimeConfig, dataSourceReactiveRuntimeConfig,
                dataSourceReactivePostgreSQLConfig);
        PgConnectOptions pgConnectOptions = toPgConnectOptions(dataSourceRuntimeConfig, dataSourceReactiveRuntimeConfig,
                dataSourceReactivePostgreSQLConfig);
        return PgPool.pool(vertx, pgConnectOptions, poolOptions);
    }

    private PoolOptions toPoolOptions(DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig) {
        PoolOptions poolOptions;
        poolOptions = new PoolOptions();

        if (dataSourceReactiveRuntimeConfig.maxSize.isPresent()) {
            poolOptions.setMaxSize(dataSourceReactiveRuntimeConfig.maxSize.getAsInt());
        }

        return poolOptions;
    }

    private PgConnectOptions toPgConnectOptions(DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig) {
        PgConnectOptions pgConnectOptions;

        if (dataSourceReactiveRuntimeConfig.url.isPresent()) {
            String url = dataSourceReactiveRuntimeConfig.url.get();
            // clean up the URL to make migrations easier
            if (url.matches("^vertx-reactive:postgre(?:s|sql)://.*$")) {
                url = url.substring("vertx-reactive:".length());
            }
            pgConnectOptions = PgConnectOptions.fromUri(url);
        } else {
            pgConnectOptions = new PgConnectOptions();
        }

        if (dataSourceRuntimeConfig.username.isPresent()) {
            pgConnectOptions.setUser(dataSourceRuntimeConfig.username.get());
        }

        if (dataSourceRuntimeConfig.password.isPresent()) {
            pgConnectOptions.setPassword(dataSourceRuntimeConfig.password.get());
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
                pgConnectOptions.setUser(user);
            }
            if (password != null) {
                pgConnectOptions.setPassword(password);
            }
        }

        if (dataSourceReactivePostgreSQLConfig.cachePreparedStatements.isPresent()) {
            pgConnectOptions.setCachePreparedStatements(dataSourceReactivePostgreSQLConfig.cachePreparedStatements.get());
        }
        if (dataSourceReactivePostgreSQLConfig.pipeliningLimit.isPresent()) {
            pgConnectOptions.setPipeliningLimit(dataSourceReactivePostgreSQLConfig.pipeliningLimit.getAsInt());
        }

        if (dataSourceReactivePostgreSQLConfig.sslMode.isPresent()) {
            pgConnectOptions.setSslMode(dataSourceReactivePostgreSQLConfig.sslMode.get());
        }

        configurePemTrustOptions(pgConnectOptions, dataSourceReactivePostgreSQLConfig.trustCertificatePem);
        configureJksTrustOptions(pgConnectOptions, dataSourceReactivePostgreSQLConfig.trustCertificateJks);
        configurePfxTrustOptions(pgConnectOptions, dataSourceReactivePostgreSQLConfig.trustCertificatePfx);

        configurePemKeyCertOptions(pgConnectOptions, dataSourceReactivePostgreSQLConfig.keyCertificatePem);
        configureJksKeyCertOptions(pgConnectOptions, dataSourceReactivePostgreSQLConfig.keyCertificateJks);
        configurePfxKeyCertOptions(pgConnectOptions, dataSourceReactivePostgreSQLConfig.keyCertificatePfx);

        return pgConnectOptions;
    }

    // Legacy configuration

    private PgPool legacyInitialize(Vertx vertx, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            LegacyDataSourceRuntimeConfig legacyDataSourceRuntimeConfig,
            LegacyDataSourceReactivePostgreSQLConfig legacyDataSourceReactivePostgreSQLConfig) {
        PoolOptions poolOptions = legacyToPoolOptionsLegacy(legacyDataSourceRuntimeConfig);
        PgConnectOptions pgConnectOptions = legacyToPostgreSQLConnectOptions(dataSourceRuntimeConfig,
                legacyDataSourceRuntimeConfig, legacyDataSourceReactivePostgreSQLConfig);
        return PgPool.pool(vertx, pgConnectOptions, poolOptions);
    }

    private PoolOptions legacyToPoolOptionsLegacy(LegacyDataSourceRuntimeConfig legacyDataSourceRuntimeConfig) {
        PoolOptions poolOptions;
        poolOptions = new PoolOptions();

        // Slight change of behavior compared to the legacy code: the default max size is set to 20
        poolOptions.setMaxSize(legacyDataSourceRuntimeConfig.maxSize);

        return poolOptions;
    }

    private PgConnectOptions legacyToPostgreSQLConnectOptions(DataSourceRuntimeConfig dataSourceRuntimeConfig,
            LegacyDataSourceRuntimeConfig legacyDataSourceRuntimeConfig,
            LegacyDataSourceReactivePostgreSQLConfig legacyDataSourceReactivePostgreSQLConfig) {
        PgConnectOptions pgConnectOptions;
        if (legacyDataSourceRuntimeConfig.url.isPresent()) {
            String url = legacyDataSourceRuntimeConfig.url.get();
            // clean up the URL to make migrations easier
            if (url.matches("^vertx-reactive:postgre(?:s|sql)://.*$")) {
                url = url.substring("vertx-reactive:".length());
            }
            pgConnectOptions = PgConnectOptions.fromUri(url);
        } else {
            pgConnectOptions = new PgConnectOptions();
        }

        if (dataSourceRuntimeConfig.username.isPresent()) {
            pgConnectOptions.setUser(dataSourceRuntimeConfig.username.get());
        }

        if (dataSourceRuntimeConfig.password.isPresent()) {
            pgConnectOptions.setPassword(dataSourceRuntimeConfig.password.get());
        }

        if (legacyDataSourceReactivePostgreSQLConfig.cachePreparedStatements.isPresent()) {
            pgConnectOptions.setCachePreparedStatements(legacyDataSourceReactivePostgreSQLConfig.cachePreparedStatements.get());
        }
        if (legacyDataSourceReactivePostgreSQLConfig.pipeliningLimit.isPresent()) {
            pgConnectOptions.setPipeliningLimit(legacyDataSourceReactivePostgreSQLConfig.pipeliningLimit.getAsInt());
        }

        return pgConnectOptions;
    }
}
