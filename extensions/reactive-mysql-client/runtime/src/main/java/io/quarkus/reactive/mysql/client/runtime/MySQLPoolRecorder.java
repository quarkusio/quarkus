package io.quarkus.reactive.mysql.client.runtime;

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
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;

@Recorder
@SuppressWarnings("deprecation")
public class MySQLPoolRecorder {
    public RuntimeValue<MySQLPool> configureMySQLPool(RuntimeValue<Vertx> vertx, BeanContainer container,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig,
            LegacyDataSourcesRuntimeConfig legacyDataSourcesRuntimeConfig,
            LegacyDataSourceReactiveMySQLConfig legacyDataSourceReactiveMySQLConfig,
            boolean isLegacy,
            ShutdownContext shutdown) {

        MySQLPool mysqlPool;
        if (!isLegacy) {
            mysqlPool = initialize(vertx.getValue(), dataSourcesRuntimeConfig.defaultDataSource,
                    dataSourceReactiveRuntimeConfig,
                    dataSourceReactiveMySQLConfig);
        } else {
            mysqlPool = legacyInitialize(vertx.getValue(), dataSourcesRuntimeConfig.defaultDataSource,
                    legacyDataSourcesRuntimeConfig.defaultDataSource, legacyDataSourceReactiveMySQLConfig);
        }

        MySQLPoolProducer producer = container.instance(MySQLPoolProducer.class);
        producer.initialize(mysqlPool);

        shutdown.addShutdownTask(mysqlPool::close);
        return new RuntimeValue<>(mysqlPool);
    }

    private MySQLPool initialize(Vertx vertx, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig) {
        PoolOptions poolOptions = toPoolOptions(dataSourceRuntimeConfig, dataSourceReactiveRuntimeConfig,
                dataSourceReactiveMySQLConfig);
        MySQLConnectOptions mysqlConnectOptions = toMySQLConnectOptions(dataSourceRuntimeConfig,
                dataSourceReactiveRuntimeConfig, dataSourceReactiveMySQLConfig);
        return MySQLPool.pool(vertx, mysqlConnectOptions, poolOptions);
    }

    private PoolOptions toPoolOptions(DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig) {
        PoolOptions poolOptions;
        poolOptions = new PoolOptions();

        if (dataSourceReactiveRuntimeConfig.maxSize.isPresent()) {
            poolOptions.setMaxSize(dataSourceReactiveRuntimeConfig.maxSize.getAsInt());
        }

        return poolOptions;
    }

    private MySQLConnectOptions toMySQLConnectOptions(DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig) {
        MySQLConnectOptions mysqlConnectOptions;
        if (dataSourceReactiveRuntimeConfig.url.isPresent()) {
            String url = dataSourceReactiveRuntimeConfig.url.get();
            // clean up the URL to make migrations easier
            if (url.startsWith("vertx-reactive:mysql://")) {
                url = url.substring("vertx-reactive:".length());
            }
            mysqlConnectOptions = MySQLConnectOptions.fromUri(url);
        } else {
            mysqlConnectOptions = new MySQLConnectOptions();
        }

        if (dataSourceRuntimeConfig.username.isPresent()) {
            mysqlConnectOptions.setUser(dataSourceRuntimeConfig.username.get());
        }

        if (dataSourceRuntimeConfig.password.isPresent()) {
            mysqlConnectOptions.setPassword(dataSourceRuntimeConfig.password.get());
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
                mysqlConnectOptions.setUser(user);
            }
            if (password != null) {
                mysqlConnectOptions.setPassword(password);
            }
        }

        if (dataSourceReactiveMySQLConfig.cachePreparedStatements.isPresent()) {
            mysqlConnectOptions.setCachePreparedStatements(dataSourceReactiveMySQLConfig.cachePreparedStatements.get());
        }
        if (dataSourceReactiveMySQLConfig.charset.isPresent()) {
            mysqlConnectOptions.setCharset(dataSourceReactiveMySQLConfig.charset.get());
        }
        if (dataSourceReactiveMySQLConfig.collation.isPresent()) {
            mysqlConnectOptions.setCollation(dataSourceReactiveMySQLConfig.collation.get());
        }

        if (dataSourceReactiveMySQLConfig.sslMode.isPresent()) {
            mysqlConnectOptions.setSslMode(dataSourceReactiveMySQLConfig.sslMode.get());
        }

        mysqlConnectOptions.setTrustAll(dataSourceReactiveRuntimeConfig.trustAll);

        configurePemTrustOptions(mysqlConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificatePem);
        configureJksTrustOptions(mysqlConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificateJks);
        configurePfxTrustOptions(mysqlConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificatePfx);

        configurePemKeyCertOptions(mysqlConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificatePem);
        configureJksKeyCertOptions(mysqlConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificateJks);
        configurePfxKeyCertOptions(mysqlConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificatePfx);

        return mysqlConnectOptions;
    }

    // Legacy configuration

    private MySQLPool legacyInitialize(Vertx vertx, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            LegacyDataSourceRuntimeConfig legacyDataSourceRuntimeConfig,
            LegacyDataSourceReactiveMySQLConfig legacyDataSourceReactiveMySQLConfig) {
        PoolOptions poolOptions = legacyToPoolOptionsLegacy(legacyDataSourceRuntimeConfig);
        MySQLConnectOptions mysqlConnectOptions = legacyToMySQLConnectOptions(dataSourceRuntimeConfig,
                legacyDataSourceRuntimeConfig, legacyDataSourceReactiveMySQLConfig);
        return MySQLPool.pool(vertx, mysqlConnectOptions, poolOptions);
    }

    private PoolOptions legacyToPoolOptionsLegacy(LegacyDataSourceRuntimeConfig legacyDataSourceRuntimeConfig) {
        PoolOptions poolOptions;
        poolOptions = new PoolOptions();

        // Slight change of behavior compared to the legacy code: the default max size is set to 20
        poolOptions.setMaxSize(legacyDataSourceRuntimeConfig.maxSize);

        return poolOptions;
    }

    private MySQLConnectOptions legacyToMySQLConnectOptions(DataSourceRuntimeConfig dataSourceRuntimeConfig,
            LegacyDataSourceRuntimeConfig legacyDataSourceRuntimeConfig,
            LegacyDataSourceReactiveMySQLConfig legacyDataSourceReactiveMySQLConfig) {
        MySQLConnectOptions mysqlConnectOptions;
        if (legacyDataSourceRuntimeConfig.url.isPresent()) {
            String url = legacyDataSourceRuntimeConfig.url.get();
            // clean up the URL to make migrations easier
            if (url.startsWith("vertx-reactive:mysql://")) {
                url = url.substring("vertx-reactive:".length());
            }
            mysqlConnectOptions = MySQLConnectOptions.fromUri(url);
        } else {
            mysqlConnectOptions = new MySQLConnectOptions();
        }

        if (dataSourceRuntimeConfig.username.isPresent()) {
            mysqlConnectOptions.setUser(dataSourceRuntimeConfig.username.get());
        }

        if (dataSourceRuntimeConfig.password.isPresent()) {
            mysqlConnectOptions.setPassword(dataSourceRuntimeConfig.password.get());
        }

        if (legacyDataSourceReactiveMySQLConfig.cachePreparedStatements.isPresent()) {
            mysqlConnectOptions.setCachePreparedStatements(legacyDataSourceReactiveMySQLConfig.cachePreparedStatements.get());
        }
        if (legacyDataSourceReactiveMySQLConfig.charset.isPresent()) {
            mysqlConnectOptions.setCharset(legacyDataSourceReactiveMySQLConfig.charset.get());
        }
        if (legacyDataSourceReactiveMySQLConfig.collation.isPresent()) {
            mysqlConnectOptions.setCollation(legacyDataSourceReactiveMySQLConfig.collation.get());
        }

        return mysqlConnectOptions;
    }
}
