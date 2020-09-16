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
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
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

    private static final Logger log = Logger.getLogger(MySQLPoolRecorder.class);

    public RuntimeValue<MySQLPool> configureMySQLPool(RuntimeValue<Vertx> vertx,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactiveMySQLConfig dataSourceReactiveMySQLConfig,
            ShutdownContext shutdown) {

        MySQLPool mysqlPool = initialize(vertx.getValue(), dataSourcesRuntimeConfig.defaultDataSource,
                dataSourceReactiveRuntimeConfig,
                dataSourceReactiveMySQLConfig);

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
        if (dataSourceReactiveRuntimeConfig.threadLocal.isPresent() &&
                dataSourceReactiveRuntimeConfig.threadLocal.get()) {
            return new ThreadLocalMySQLPool(vertx, mysqlConnectOptions, poolOptions);
        }
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
            log.warn(
                    "datasource.reactive.mysql.cache-prepared-statements is deprecated, use datasource.reactive.cache-prepared-statements instead");
            mysqlConnectOptions.setCachePreparedStatements(dataSourceReactiveMySQLConfig.cachePreparedStatements.get());
        } else {
            mysqlConnectOptions.setCachePreparedStatements(dataSourceReactiveRuntimeConfig.cachePreparedStatements);
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

        mysqlConnectOptions.setReconnectAttempts(dataSourceReactiveRuntimeConfig.reconnectAttempts);

        mysqlConnectOptions.setReconnectInterval(dataSourceReactiveRuntimeConfig.reconnectInterval.toMillis());

        if (dataSourceReactiveRuntimeConfig.idleTimeout.isPresent()) {
            int idleTimeout = Math.toIntExact(dataSourceReactiveRuntimeConfig.idleTimeout.get().toMillis());
            mysqlConnectOptions.setIdleTimeout(idleTimeout).setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
        }

        return mysqlConnectOptions;
    }

}
