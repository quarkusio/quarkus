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
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveRuntimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourcesReactiveRuntimeConfig;
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

    private static final Logger log = Logger.getLogger(PgPoolRecorder.class);

    public RuntimeValue<PgPool> configurePgPool(RuntimeValue<Vertx> vertx,
            String dataSourceName,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesReactiveRuntimeConfig dataSourcesReactiveRuntimeConfig,
            DataSourcesReactivePostgreSQLConfig dataSourcesReactivePostgreSQLConfig,
            ShutdownContext shutdown) {

        PgPool pgPool = initialize(vertx.getValue(),
                dataSourcesRuntimeConfig.getDataSourceRuntimeConfig(dataSourceName),
                dataSourcesReactiveRuntimeConfig.getDataSourceReactiveRuntimeConfig(dataSourceName),
                dataSourcesReactivePostgreSQLConfig.getDataSourceReactiveRuntimeConfig(dataSourceName));

        shutdown.addShutdownTask(pgPool::close);
        return new RuntimeValue<>(pgPool);
    }

    public RuntimeValue<io.vertx.mutiny.pgclient.PgPool> mutinyPgPool(RuntimeValue<PgPool> pgPool) {
        return new RuntimeValue<>(io.vertx.mutiny.pgclient.PgPool.newInstance(pgPool.getValue()));
    }

    private PgPool initialize(Vertx vertx, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceReactiveRuntimeConfig dataSourceReactiveRuntimeConfig,
            DataSourceReactivePostgreSQLConfig dataSourceReactivePostgreSQLConfig) {
        PoolOptions poolOptions = toPoolOptions(dataSourceRuntimeConfig, dataSourceReactiveRuntimeConfig,
                dataSourceReactivePostgreSQLConfig);
        PgConnectOptions pgConnectOptions = toPgConnectOptions(dataSourceRuntimeConfig, dataSourceReactiveRuntimeConfig,
                dataSourceReactivePostgreSQLConfig);
        if (dataSourceReactiveRuntimeConfig.threadLocal.isPresent() &&
                dataSourceReactiveRuntimeConfig.threadLocal.get()) {
            return new ThreadLocalPgPool(vertx, pgConnectOptions, poolOptions);
        }
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
            log.warn(
                    "datasource.reactive.postgresql.cache-prepared-statements is deprecated, use datasource.reactive.cache-prepared-statements instead");
            pgConnectOptions.setCachePreparedStatements(dataSourceReactivePostgreSQLConfig.cachePreparedStatements.get());
        } else {
            pgConnectOptions.setCachePreparedStatements(dataSourceReactiveRuntimeConfig.cachePreparedStatements);
        }

        if (dataSourceReactivePostgreSQLConfig.pipeliningLimit.isPresent()) {
            pgConnectOptions.setPipeliningLimit(dataSourceReactivePostgreSQLConfig.pipeliningLimit.getAsInt());
        }

        if (dataSourceReactivePostgreSQLConfig.sslMode.isPresent()) {
            pgConnectOptions.setSslMode(dataSourceReactivePostgreSQLConfig.sslMode.get());
        }

        pgConnectOptions.setTrustAll(dataSourceReactiveRuntimeConfig.trustAll);

        configurePemTrustOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificatePem);
        configureJksTrustOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificateJks);
        configurePfxTrustOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.trustCertificatePfx);

        configurePemKeyCertOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificatePem);
        configureJksKeyCertOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificateJks);
        configurePfxKeyCertOptions(pgConnectOptions, dataSourceReactiveRuntimeConfig.keyCertificatePfx);

        pgConnectOptions.setReconnectAttempts(dataSourceReactiveRuntimeConfig.reconnectAttempts);

        pgConnectOptions.setReconnectInterval(dataSourceReactiveRuntimeConfig.reconnectInterval.toMillis());

        if (dataSourceReactiveRuntimeConfig.idleTimeout.isPresent()) {
            int idleTimeout = Math.toIntExact(dataSourceReactiveRuntimeConfig.idleTimeout.get().toMillis());
            pgConnectOptions.setIdleTimeout(idleTimeout).setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
        }

        return pgConnectOptions;
    }
}
