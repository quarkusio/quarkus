package io.quarkus.reactive.datasource.runtime;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;
import static io.quarkus.reactive.datasource.runtime.UnitisedTime.unitised;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jboss.logging.Logger;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.vertx.core.runtime.config.JksConfiguration;
import io.quarkus.vertx.core.runtime.config.PemKeyCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PemTrustCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PfxConfiguration;
import io.vertx.core.Future;
import io.vertx.core.net.ClientSSLOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.impl.Utils;

/**
 * Shared utility methods for reactive SQL client pool configuration.
 * Used by all DB-specific recorders (PG, MySQL, MSSQL, Oracle, DB2).
 */
public final class ReactivePoolUtil {

    private static final Logger log = Logger.getLogger(ReactivePoolUtil.class);

    private ReactivePoolUtil() {
    }

    /**
     * Build {@link PoolOptions} from the generic reactive datasource config.
     */
    public static PoolOptions toPoolOptions(Integer eventLoopCount,
            DataSourceReactiveRuntimeConfig config) {
        PoolOptions poolOptions = new PoolOptions();

        poolOptions.setMaxSize(config.maxSize());

        if (config.connectionTimeout().isPresent()) {
            var connectionTimeout = unitised(config.connectionTimeout().get());
            poolOptions.setConnectionTimeout(connectionTimeout.value).setConnectionTimeoutUnit(connectionTimeout.unit);
        }

        if (config.maxWaitQueueSize().isPresent()) {
            poolOptions.setMaxWaitQueueSize(config.maxWaitQueueSize().getAsInt());
        }

        if (config.poolCleanerPeriod().isPresent()) {
            var cleanerPeriod = unitised(config.poolCleanerPeriod().get());
            poolOptions.setPoolCleanerPeriod((int) MILLISECONDS.convert(cleanerPeriod.value, cleanerPeriod.unit));
        }

        if (config.idleTimeout().isPresent()) {
            var idleTimeout = unitised(config.idleTimeout().get());
            poolOptions.setIdleTimeout(idleTimeout.value).setIdleTimeoutUnit(idleTimeout.unit);
        }

        if (config.maxLifetime().isPresent()) {
            var maxLifetime = unitised(config.maxLifetime().get());
            poolOptions.setMaxLifetime(maxLifetime.value).setMaxLifetimeUnit(maxLifetime.unit);
        }

        if (config.shared()) {
            poolOptions.setShared(true);
            if (config.name().isPresent()) {
                poolOptions.setName(config.name().get());
            }
        }

        if (config.eventLoopSize().isPresent()) {
            poolOptions.setEventLoopSize(Math.max(0, config.eventLoopSize().getAsInt()));
        } else if (eventLoopCount != null) {
            poolOptions.setEventLoopSize(Math.max(0, eventLoopCount));
        }

        return poolOptions;
    }

    /**
     * Build the {@link NetClientOptions} used by the pool to open connections, from the TCP keep-alive and socket idle
     * timeout settings of the generic reactive datasource config.
     * <p>
     * When none of these settings is configured, the result is equivalent to the default {@link NetClientOptions}.
     */
    public static NetClientOptions toNetClientOptions(DataSourceReactiveRuntimeConfig config) {
        NetClientOptions netClientOptions = new NetClientOptions();

        netClientOptions.setTcpKeepAlive(config.tcpKeepAlive());
        if (config.tcpKeepAliveIdle().isPresent()) {
            netClientOptions.setTcpKeepAliveIdleSeconds(toSeconds("tcp-keep-alive-idle", config.tcpKeepAliveIdle().get()));
        }
        if (config.tcpKeepAliveInterval().isPresent()) {
            netClientOptions.setTcpKeepAliveIntervalSeconds(
                    toSeconds("tcp-keep-alive-interval", config.tcpKeepAliveInterval().get()));
        }
        if (config.tcpKeepAliveCount().isPresent()) {
            int count = config.tcpKeepAliveCount().getAsInt();
            if (count < 1) {
                throw new ConfigurationException("tcp-keep-alive-count must be strictly positive, got " + count);
            }
            netClientOptions.setTcpKeepAliveCount(count);
        }

        if (config.socketReadIdleTimeout().isPresent() || config.socketWriteIdleTimeout().isPresent()) {
            // Vert.x uses a single unit for the read, write and read/write idle timeouts
            netClientOptions.setIdleTimeoutUnit(MILLISECONDS);
            if (config.socketReadIdleTimeout().isPresent()) {
                netClientOptions
                        .setReadIdleTimeout(toMillis("socket-read-idle-timeout", config.socketReadIdleTimeout().get()));
            }
            if (config.socketWriteIdleTimeout().isPresent()) {
                netClientOptions
                        .setWriteIdleTimeout(toMillis("socket-write-idle-timeout", config.socketWriteIdleTimeout().get()));
            }
        }

        return netClientOptions;
    }

    private static int toSeconds(String property, Duration duration) {
        long seconds = duration.toSeconds();
        if (seconds < 1 || seconds > Integer.MAX_VALUE) {
            throw new ConfigurationException(property + " must be between 1 second and " + Integer.MAX_VALUE
                    + " seconds, got " + duration);
        }
        return (int) seconds;
    }

    private static int toMillis(String property, Duration duration) {
        if (duration.isNegative() || duration.isZero() || duration.toMillis() > Integer.MAX_VALUE) {
            throw new ConfigurationException(property + " must be between 1 millisecond and " + Integer.MAX_VALUE
                    + " milliseconds, got " + duration);
        }
        return (int) Math.max(1, duration.toMillis());
    }

    /**
     * Build a databases supplier that handles credential rotation via {@link CredentialsProvider}
     * or falls back to round-robin for multiple connect options.
     *
     * @param connectOptionsList the base connect options
     * @param dataSourceRuntimeConfig the datasource runtime config
     * @param copyConstructor a function that copies a connect options instance (e.g. {@code PgConnectOptions::new})
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <CO extends SqlConnectOptions> Supplier<Future<SqlConnectOptions>> toDatabasesSupplier(
            List<CO> connectOptionsList,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            UnaryOperator<CO> copyConstructor) {
        if (dataSourceRuntimeConfig.credentialsProvider().isPresent()) {
            String beanName = dataSourceRuntimeConfig.credentialsProviderName().orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = dataSourceRuntimeConfig.credentialsProvider().get();
            return (Supplier) new ConnectOptionsSupplier<>(credentialsProvider, name, connectOptionsList, copyConstructor);
        } else {
            return (Supplier) Utils.roundRobinSupplier(connectOptionsList);
        }
    }

    /**
     * Apply credentials from the datasource config and credentials provider to connect options.
     */
    public static void configureCredentials(SqlConnectOptions connectOptions,
            DataSourceRuntimeConfig dataSourceRuntimeConfig) {
        dataSourceRuntimeConfig.username().ifPresent(connectOptions::setUser);
        dataSourceRuntimeConfig.password().ifPresent(connectOptions::setPassword);

        if (dataSourceRuntimeConfig.credentialsProvider().isPresent()) {
            String beanName = dataSourceRuntimeConfig.credentialsProviderName().orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
            String name = dataSourceRuntimeConfig.credentialsProvider().get();
            Map<String, String> credentials = credentialsProvider.getCredentialsAsync(name).await().indefinitely();
            String user = credentials.get(USER_PROPERTY_NAME);
            String password = credentials.get(PASSWORD_PROPERTY_NAME);
            if (user != null) {
                connectOptions.setUser(user);
            }
            if (password != null) {
                connectOptions.setPassword(password);
            }
        }
    }

    /**
     * Apply all prepared statement cache settings from the generic reactive datasource config to connect options.
     * <p>
     * Only call this for SQL clients that support prepared statement caching.
     */
    public static void configurePreparedStatementCache(SqlConnectOptions connectOptions,
            DataSourceReactiveRuntimeConfig config) {
        connectOptions.setCachePreparedStatements(config.cachePreparedStatements().orElse(true));
        if (config.preparedStatementCacheMaxSize().isPresent()) {
            connectOptions.setPreparedStatementCacheMaxSize(config.preparedStatementCacheMaxSize().getAsInt());
        }
        if (config.preparedStatementCacheSqlLimit().isPresent()) {
            connectOptions.setPreparedStatementCacheSqlLimit(config.preparedStatementCacheSqlLimit().getAsInt());
        }
    }

    /**
     * Apply SSL/TLS, reconnect, and hostname verification settings from the generic reactive
     * datasource config to connect options.
     * <p>
     * When a named TLS configuration is set via {@code tls-configuration-name}, it takes precedence
     * over manual SSL properties (trust-certificate-*, key-certificate-*, trust-all, hostname-verification-algorithm).
     */
    public static void configureSsl(SqlConnectOptions connectOptions,
            DataSourceReactiveRuntimeConfig config,
            TlsConfigurationRegistry tlsRegistry) {
        if (config.tlsConfigurationName().isPresent()) {
            String tlsConfigName = config.tlsConfigurationName().get();
            if (tlsRegistry == null) {
                throw new ConfigurationException(
                        "TLS configuration name '" + tlsConfigName + "' is set but the TLS registry is not available.");
            }
            Optional<TlsConfiguration> maybeTlsConfig = tlsRegistry.get(tlsConfigName);
            if (maybeTlsConfig.isEmpty()) {
                throw new ConfigurationException("Unable to find the TLS configuration '" + tlsConfigName
                        + "' for the reactive datasource.");
            }
            TlsConfiguration tlsConfig = maybeTlsConfig.get();
            ClientSSLOptions sslOptions = tlsConfig.getClientSSLOptions();
            if (sslOptions == null) {
                sslOptions = new ClientSSLOptions();
            }
            connectOptions.setSslOptions(sslOptions);

            if (hasManualSslProperties(config)) {
                log.warn("Manual SSL properties (trust-certificate-*, key-certificate-*, trust-all,"
                        + " hostname-verification-algorithm) are ignored when a named TLS configuration"
                        + " (tls-configuration-name=" + tlsConfigName + ") is set.");
            }
        } else {
            configureManualSsl(connectOptions, config);
        }

        connectOptions.setReconnectAttempts(config.reconnectAttempts());
        connectOptions.setReconnectInterval(config.reconnectInterval().toMillis());
    }

    private static boolean hasManualSslProperties(DataSourceReactiveRuntimeConfig config) {
        return config.trustAll()
                || config.trustCertificatePem().enabled()
                || (config.trustCertificatePem().certs().isPresent() && !config.trustCertificatePem().certs().get().isEmpty())
                || config.trustCertificateJks().enabled()
                || config.trustCertificatePfx().enabled()
                || config.keyCertificatePem().enabled()
                || config.keyCertificateJks().enabled()
                || config.keyCertificatePfx().enabled()
                || !"NONE".equalsIgnoreCase(config.hostnameVerificationAlgorithm());
    }

    private static void configureManualSsl(SqlConnectOptions connectOptions,
            DataSourceReactiveRuntimeConfig config) {
        ClientSSLOptions sslOptions = connectOptions.getSslOptions();
        if (sslOptions == null) {
            sslOptions = new ClientSSLOptions();
        }

        sslOptions.setTrustAll(config.trustAll());

        configurePemTrustOptions(sslOptions, config.trustCertificatePem());
        configureJksTrustOptions(sslOptions, config.trustCertificateJks());
        configurePfxTrustOptions(sslOptions, config.trustCertificatePfx());

        configurePemKeyCertOptions(sslOptions, config.keyCertificatePem());
        configureJksKeyCertOptions(sslOptions, config.keyCertificateJks());
        configurePfxKeyCertOptions(sslOptions, config.keyCertificatePfx());

        String algo = config.hostnameVerificationAlgorithm();
        if ("NONE".equalsIgnoreCase(algo)) {
            sslOptions.setHostnameVerificationAlgorithm("");
        } else {
            sslOptions.setHostnameVerificationAlgorithm(algo);
        }

        connectOptions.setSslOptions(sslOptions);
    }

    /**
     * @deprecated Use {@link #configureSsl(SqlConnectOptions, DataSourceReactiveRuntimeConfig, TlsConfigurationRegistry)}
     *             to support the TLS registry.
     */
    @Deprecated(forRemoval = true)
    public static void configureSsl(SqlConnectOptions connectOptions,
            DataSourceReactiveRuntimeConfig config) {
        configureManualSsl(connectOptions, config);
        connectOptions.setReconnectAttempts(config.reconnectAttempts());
        connectOptions.setReconnectInterval(config.reconnectInterval().toMillis());
    }

    private static void configurePemTrustOptions(ClientSSLOptions options, PemTrustCertConfiguration configuration) {
        if (configuration.enabled() || (configuration.certs().isPresent() && !configuration.certs().get().isEmpty())) {
            PemTrustOptions pemTrustOptions = new PemTrustOptions();
            if (configuration.certs().isPresent()) {
                for (String cert : configuration.certs().get()) {
                    pemTrustOptions.addCertPath(cert);
                }
            }
            options.setTrustOptions(pemTrustOptions);
        }
    }

    private static void configureJksTrustOptions(ClientSSLOptions options, JksConfiguration configuration) {
        if (configuration.enabled()) {
            options.setTrustOptions(toJksOptions(configuration));
        }
    }

    private static void configurePfxTrustOptions(ClientSSLOptions options, PfxConfiguration configuration) {
        if (configuration.enabled()) {
            options.setTrustOptions(toPfxOptions(configuration));
        }
    }

    private static void configurePemKeyCertOptions(ClientSSLOptions options, PemKeyCertConfiguration configuration) {
        if (configuration.enabled()) {
            PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
            if (configuration.certs().isPresent()) {
                for (String cert : configuration.certs().get()) {
                    pemKeyCertOptions.addCertPath(cert);
                }
            }
            if (configuration.keys().isPresent()) {
                for (String key : configuration.keys().get()) {
                    pemKeyCertOptions.addKeyPath(key);
                }
            }
            options.setKeyCertOptions(pemKeyCertOptions);
        }
    }

    private static void configureJksKeyCertOptions(ClientSSLOptions options, JksConfiguration configuration) {
        if (configuration.enabled()) {
            options.setKeyCertOptions(toJksOptions(configuration));
        }
    }

    private static void configurePfxKeyCertOptions(ClientSSLOptions options, PfxConfiguration configuration) {
        if (configuration.enabled()) {
            options.setKeyCertOptions(toPfxOptions(configuration));
        }
    }

    private static JksOptions toJksOptions(JksConfiguration configuration) {
        JksOptions jksOptions = new JksOptions();
        configuration.path().ifPresent(jksOptions::setPath);
        configuration.password().ifPresent(jksOptions::setPassword);
        return jksOptions;
    }

    private static PfxOptions toPfxOptions(PfxConfiguration configuration) {
        PfxOptions pfxOptions = new PfxOptions();
        configuration.path().ifPresent(pfxOptions::setPath);
        configuration.password().ifPresent(pfxOptions::setPassword);
        return pfxOptions;
    }
}
