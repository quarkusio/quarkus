package io.quarkus.agroal.runtime;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration.ConnectionValidator;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.api.transaction.TransactionIntegration;
import io.agroal.narayana.NarayanaTransactionIntegration;
import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig.DataSourceJdbcOuterNamedBuildTimeConfig;
import io.quarkus.agroal.runtime.DataSourcesJdbcRuntimeConfig.DataSourceJdbcOuterNamedRuntimeConfig;
import io.quarkus.agroal.runtime.JdbcDriver.JdbcDriverLiteral;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.datasource.runtime.LegacyDataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.LegacyDataSourcesRuntimeConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vault.CredentialsProvider;

@SuppressWarnings("deprecation")
public abstract class AbstractDataSourceProducer {

    private static final Logger log = Logger.getLogger(AbstractDataSourceProducer.class.getName());

    private DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig;
    private DataSourcesRuntimeConfig dataSourcesRuntimeConfig;
    private DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig;
    private DataSourcesJdbcRuntimeConfig dataSourcesJdbcRuntimeConfig;

    private LegacyDataSourcesJdbcBuildTimeConfig legacyDataSourcesJdbcBuildTimeConfig;
    private LegacyDataSourcesRuntimeConfig legacyDataSourcesRuntimeConfig;
    private LegacyDataSourcesJdbcRuntimeConfig legacyDataSourcesJdbcRuntimeConfig;

    private boolean disableSslSupport = false;

    private List<AgroalDataSource> dataSources = new ArrayList<>();

    @Inject
    public TransactionManager transactionManager;

    @Inject
    public TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public void configureDataSources(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesJdbcRuntimeConfig dataSourcesJdbcRuntimeConfig,
            LegacyDataSourcesJdbcBuildTimeConfig legacyDataSourcesJdbcBuildTimeConfig,
            LegacyDataSourcesRuntimeConfig legacyDataSourcesRuntimeConfig,
            LegacyDataSourcesJdbcRuntimeConfig legacyDataSourcesJdbcRuntimeConfig,
            boolean disableSslSupport) {
        this.dataSourcesBuildTimeConfig = dataSourcesBuildTimeConfig;
        this.dataSourcesJdbcBuildTimeConfig = dataSourcesJdbcBuildTimeConfig;
        this.dataSourcesRuntimeConfig = dataSourcesRuntimeConfig;
        this.dataSourcesJdbcRuntimeConfig = dataSourcesJdbcRuntimeConfig;

        this.legacyDataSourcesJdbcBuildTimeConfig = legacyDataSourcesJdbcBuildTimeConfig;
        this.legacyDataSourcesRuntimeConfig = legacyDataSourcesRuntimeConfig;
        this.legacyDataSourcesJdbcRuntimeConfig = legacyDataSourcesJdbcRuntimeConfig;

        this.disableSslSupport = disableSslSupport;
    }

    public AgroalDataSource createDataSource(String dataSourceName,
            DataSourceBuildTimeConfig dataSourceBuildTimeConfig,
            DataSourceJdbcBuildTimeConfig dataSourceJdbcBuildTimeConfig,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceJdbcRuntimeConfig dataSourceJdbcRuntimeConfig,
            LegacyDataSourceJdbcBuildTimeConfig legacyDataSourceJdbcBuildTimeConfig,
            LegacyDataSourceRuntimeConfig legacyDataSourceRuntimeConfig,
            LegacyDataSourceJdbcRuntimeConfig legacyDataSourceJdbcRuntimeConfig,
            String resolvedDbKind,
            String resolvedDriverClass,
            boolean mpMetricsPresent,
            boolean isLegacy) {
        checkConfigInjection();

        if (!isLegacy) {
            if (!dataSourceJdbcRuntimeConfig.url.isPresent()) {
                throw new ConfigurationException("URL is not defined for datasource " + dataSourceName);
            }
        } else {
            if (!legacyDataSourceRuntimeConfig.url.isPresent()) {
                throw new ConfigurationException("URL is not defined for datasource " + dataSourceName);
            }
        }

        // we first make sure that all available JDBC drivers are loaded in the current TCCL
        loadDriversInTCCL();

        Class<?> driver;
        try {
            driver = Class.forName(resolvedDriverClass, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Unable to load the datasource driver " + resolvedDriverClass + " for datasource " + dataSourceName, e);
        }

        InstanceHandle<AgroalConnectionConfigurer> agroalConnectionConfigurerHandle = Arc.container().instance(
                AgroalConnectionConfigurer.class,
                new JdbcDriverLiteral(resolvedDbKind));

        AgroalDataSourceConfigurationSupplier dataSourceConfiguration = new AgroalDataSourceConfigurationSupplier();

        AgroalConnectionPoolConfigurationSupplier poolConfiguration = dataSourceConfiguration.connectionPoolConfiguration();
        AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration = poolConfiguration
                .connectionFactoryConfiguration();

        if (!isLegacy) {
            applyNewConfiguration(dataSourceConfiguration, poolConfiguration, connectionFactoryConfiguration, driver,
                    dataSourceJdbcBuildTimeConfig, dataSourceRuntimeConfig, dataSourceJdbcRuntimeConfig, mpMetricsPresent);
        } else {
            applyLegacyConfiguration(dataSourceConfiguration, poolConfiguration, connectionFactoryConfiguration, driver,
                    dataSourceRuntimeConfig, legacyDataSourceJdbcBuildTimeConfig, legacyDataSourceRuntimeConfig,
                    legacyDataSourceJdbcRuntimeConfig, mpMetricsPresent);
        }

        if (disableSslSupport) {
            if (agroalConnectionConfigurerHandle.isAvailable()) {
                agroalConnectionConfigurerHandle.get().disableSslSupport(resolvedDbKind,
                        dataSourceConfiguration);
            } else {
                log.warnv("Agroal does not support disabling SSL for database kind: {0}",
                        resolvedDbKind);
            }
        }

        // Explicit reference to bypass reflection need of the ServiceLoader used by AgroalDataSource#from
        AgroalDataSourceConfiguration agroalConfiguration = dataSourceConfiguration.get();
        AgroalDataSource dataSource = new io.agroal.pool.DataSource(agroalConfiguration,
                new AgroalEventLoggingListener(dataSourceName));
        log.debugv("Started datasource {0} connected to {1}", dataSourceName,
                agroalConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl());

        this.dataSources.add(dataSource);

        return dataSource;
    }

    private void applyNewConfiguration(AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            AgroalConnectionPoolConfigurationSupplier poolConfiguration,
            AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration, Class<?> driver,
            DataSourceJdbcBuildTimeConfig dataSourceJdbcBuildTimeConfig, DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceJdbcRuntimeConfig dataSourceJdbcRuntimeConfig, boolean mpMetricsPresent) {
        connectionFactoryConfiguration.jdbcUrl(dataSourceJdbcRuntimeConfig.url.get());
        connectionFactoryConfiguration.connectionProviderClass(driver);
        connectionFactoryConfiguration.trackJdbcResources(dataSourceJdbcRuntimeConfig.detectStatementLeaks);

        if (dataSourceJdbcRuntimeConfig.transactionIsolationLevel.isPresent()) {
            connectionFactoryConfiguration
                    .jdbcTransactionIsolation(
                            dataSourceJdbcRuntimeConfig.transactionIsolationLevel.get());
        }

        if (dataSourceJdbcBuildTimeConfig.transactions != io.quarkus.agroal.runtime.TransactionIntegration.DISABLED) {
            TransactionIntegration txIntegration = new NarayanaTransactionIntegration(transactionManager,
                    transactionSynchronizationRegistry);
            poolConfiguration.transactionIntegration(txIntegration);
        }

        // New connection SQL
        if (dataSourceJdbcRuntimeConfig.newConnectionSql.isPresent()) {
            connectionFactoryConfiguration.initialSql(dataSourceJdbcRuntimeConfig.newConnectionSql.get());
        }

        // metrics
        if (dataSourceJdbcBuildTimeConfig.enableMetrics.isPresent()) {
            dataSourceConfiguration.metricsEnabled(dataSourceJdbcBuildTimeConfig.enableMetrics.get());
        } else {
            // if the enable-metrics property is unspecified, treat it as true if MP Metrics are being exposed
            dataSourceConfiguration.metricsEnabled(dataSourcesBuildTimeConfig.metricsEnabled && mpMetricsPresent);
        }

        // Authentication
        if (dataSourceRuntimeConfig.username.isPresent()) {
            connectionFactoryConfiguration
                    .principal(new NamePrincipal(dataSourceRuntimeConfig.username.get()));
        }
        if (dataSourceRuntimeConfig.password.isPresent()) {
            connectionFactoryConfiguration
                    .credential(new SimplePassword(dataSourceRuntimeConfig.password.get()));
        }

        // Vault credentials provider
        if (dataSourceRuntimeConfig.credentialsProvider.isPresent()) {
            ArcContainer container = Arc.container();
            String type = dataSourceRuntimeConfig.credentialsProviderType.orElse(null);
            CredentialsProvider credentialsProvider = type != null
                    ? (CredentialsProvider) container.instance(type).get()
                    : container.instance(CredentialsProvider.class).get();

            if (credentialsProvider == null) {
                throw new RuntimeException("unable to find credentials provider of type " + (type == null ? "default" : type));
            }

            String name = dataSourceRuntimeConfig.credentialsProvider.get();
            connectionFactoryConfiguration
                    .credential(new AgroalVaultCredentialsProviderPassword(name, credentialsProvider));
        }

        // Pool size configuration:
        poolConfiguration.minSize(dataSourceJdbcRuntimeConfig.minSize);
        poolConfiguration.maxSize(dataSourceJdbcRuntimeConfig.maxSize);
        if (dataSourceJdbcRuntimeConfig.initialSize.isPresent() && dataSourceJdbcRuntimeConfig.initialSize.getAsInt() > 0) {
            poolConfiguration.initialSize(dataSourceJdbcRuntimeConfig.initialSize.getAsInt());
        }

        // Connection management
        poolConfiguration.connectionValidator(ConnectionValidator.defaultValidator());
        if (dataSourceJdbcRuntimeConfig.acquisitionTimeout.isPresent()) {
            poolConfiguration.acquisitionTimeout(dataSourceJdbcRuntimeConfig.acquisitionTimeout.get());
        }
        if (dataSourceJdbcRuntimeConfig.backgroundValidationInterval.isPresent()) {
            poolConfiguration.validationTimeout(dataSourceJdbcRuntimeConfig.backgroundValidationInterval.get());
        }
        if (dataSourceJdbcRuntimeConfig.validationQuerySql.isPresent()) {
            String validationQuery = dataSourceJdbcRuntimeConfig.validationQuerySql.get();
            poolConfiguration.connectionValidator(new ConnectionValidator() {

                @Override
                public boolean isValid(Connection connection) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(validationQuery);
                        return true;
                    } catch (Exception e) {
                        log.warn("Connection validation failed", e);
                    }
                    return false;
                }
            });
        }
        if (dataSourceJdbcRuntimeConfig.idleRemovalInterval.isPresent()) {
            poolConfiguration.reapTimeout(dataSourceJdbcRuntimeConfig.idleRemovalInterval.get());
        }
        if (dataSourceJdbcRuntimeConfig.leakDetectionInterval.isPresent()) {
            poolConfiguration.leakTimeout(dataSourceJdbcRuntimeConfig.leakDetectionInterval.get());
        }
        if (dataSourceJdbcRuntimeConfig.maxLifetime.isPresent()) {
            poolConfiguration.maxLifetime(dataSourceJdbcRuntimeConfig.maxLifetime.get());
        }
    }

    private void applyLegacyConfiguration(AgroalDataSourceConfigurationSupplier dataSourceConfiguration,
            AgroalConnectionPoolConfigurationSupplier poolConfiguration,
            AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration, Class<?> driver,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            LegacyDataSourceJdbcBuildTimeConfig legacyDataSourceJdbcBuildTimeConfig,
            LegacyDataSourceRuntimeConfig legacyDataSourceRuntimeConfig,
            LegacyDataSourceJdbcRuntimeConfig legacyDataSourceJdbcRuntimeConfig, boolean mpMetricsPresent) {
        connectionFactoryConfiguration.jdbcUrl(legacyDataSourceRuntimeConfig.url.get());
        connectionFactoryConfiguration.connectionProviderClass(driver);
        connectionFactoryConfiguration.trackJdbcResources(legacyDataSourceJdbcRuntimeConfig.detectStatementLeaks);

        if (legacyDataSourceJdbcRuntimeConfig.transactionIsolationLevel.isPresent()) {
            connectionFactoryConfiguration
                    .jdbcTransactionIsolation(
                            legacyDataSourceJdbcRuntimeConfig.transactionIsolationLevel.get());
        }

        if (legacyDataSourceJdbcBuildTimeConfig.transactions != io.quarkus.agroal.runtime.TransactionIntegration.DISABLED) {
            TransactionIntegration txIntegration = new NarayanaTransactionIntegration(transactionManager,
                    transactionSynchronizationRegistry);
            poolConfiguration.transactionIntegration(txIntegration);
        }

        // New connection SQL
        if (legacyDataSourceJdbcRuntimeConfig.newConnectionSql.isPresent()) {
            connectionFactoryConfiguration.initialSql(legacyDataSourceJdbcRuntimeConfig.newConnectionSql.get());
        }

        // metrics
        if (legacyDataSourceJdbcBuildTimeConfig.enableMetrics.isPresent()) {
            dataSourceConfiguration.metricsEnabled(legacyDataSourceJdbcBuildTimeConfig.enableMetrics.get());
        } else {
            // if the enable-metrics property is unspecified, treat it as true if MP Metrics are being exposed
            dataSourceConfiguration.metricsEnabled(dataSourcesBuildTimeConfig.metricsEnabled && mpMetricsPresent);
        }

        // Authentication
        if (dataSourceRuntimeConfig.username.isPresent()) {
            connectionFactoryConfiguration
                    .principal(new NamePrincipal(dataSourceRuntimeConfig.username.get()));
        }
        if (dataSourceRuntimeConfig.password.isPresent()) {
            connectionFactoryConfiguration
                    .credential(new SimplePassword(dataSourceRuntimeConfig.password.get()));
        }

        // Vault credentials provider
        if (dataSourceRuntimeConfig.credentialsProvider.isPresent()) {
            ArcContainer container = Arc.container();
            String type = dataSourceRuntimeConfig.credentialsProviderType.orElse(null);
            CredentialsProvider credentialsProvider = type != null
                    ? (CredentialsProvider) container.instance(type).get()
                    : container.instance(CredentialsProvider.class).get();

            if (credentialsProvider == null) {
                throw new RuntimeException("unable to find credentials provider of type " + (type == null ? "default" : type));
            }

            String name = dataSourceRuntimeConfig.credentialsProvider.get();
            connectionFactoryConfiguration
                    .credential(new AgroalVaultCredentialsProviderPassword(name, credentialsProvider));
        }

        // Pool size configuration:
        poolConfiguration.minSize(legacyDataSourceJdbcRuntimeConfig.minSize);
        poolConfiguration.maxSize(legacyDataSourceRuntimeConfig.maxSize);
        if (legacyDataSourceJdbcRuntimeConfig.initialSize.isPresent()
                && legacyDataSourceJdbcRuntimeConfig.initialSize.get() > 0) {
            poolConfiguration.initialSize(legacyDataSourceJdbcRuntimeConfig.initialSize.get());
        }

        // Connection management
        poolConfiguration.connectionValidator(ConnectionValidator.defaultValidator());
        if (legacyDataSourceJdbcRuntimeConfig.acquisitionTimeout.isPresent()) {
            poolConfiguration.acquisitionTimeout(legacyDataSourceJdbcRuntimeConfig.acquisitionTimeout.get());
        }
        if (legacyDataSourceJdbcRuntimeConfig.backgroundValidationInterval.isPresent()) {
            poolConfiguration.validationTimeout(legacyDataSourceJdbcRuntimeConfig.backgroundValidationInterval.get());
        }
        if (legacyDataSourceJdbcRuntimeConfig.validationQuerySql.isPresent()) {
            String validationQuery = legacyDataSourceJdbcRuntimeConfig.validationQuerySql.get();
            poolConfiguration.connectionValidator(new ConnectionValidator() {

                @Override
                public boolean isValid(Connection connection) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(validationQuery);
                        return true;
                    } catch (Exception e) {
                        log.warn("Connection validation failed", e);
                    }
                    return false;
                }
            });
        }
        if (legacyDataSourceJdbcRuntimeConfig.idleRemovalInterval.isPresent()) {
            poolConfiguration.reapTimeout(legacyDataSourceJdbcRuntimeConfig.idleRemovalInterval.get());
        }
        if (legacyDataSourceJdbcRuntimeConfig.leakDetectionInterval.isPresent()) {
            poolConfiguration.leakTimeout(legacyDataSourceJdbcRuntimeConfig.leakDetectionInterval.get());
        }
        if (legacyDataSourceJdbcRuntimeConfig.maxLifetime.isPresent()) {
            poolConfiguration.maxLifetime(legacyDataSourceJdbcRuntimeConfig.maxLifetime.get());
        }
    }

    public DataSourceBuildTimeConfig getDataSourceBuildTimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return dataSourcesBuildTimeConfig.defaultDataSource;
        }

        DataSourceBuildTimeConfig namedConfig = dataSourcesBuildTimeConfig.namedDataSources.get(dataSourceName);

        return namedConfig != null ? namedConfig : new DataSourceBuildTimeConfig();
    }

    public DataSourceJdbcBuildTimeConfig getDataSourceJdbcBuildTimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return dataSourcesJdbcBuildTimeConfig.jdbc;
        }

        DataSourceJdbcOuterNamedBuildTimeConfig namedOuterConfig = dataSourcesJdbcBuildTimeConfig.namedDataSources
                .get(dataSourceName);

        return namedOuterConfig != null ? namedOuterConfig.jdbc : new DataSourceJdbcBuildTimeConfig();
    }

    public DataSourceRuntimeConfig getDataSourceRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return dataSourcesRuntimeConfig.defaultDataSource;
        }

        DataSourceRuntimeConfig namedConfig = dataSourcesRuntimeConfig.namedDataSources.get(dataSourceName);

        return namedConfig != null ? namedConfig : new DataSourceRuntimeConfig();
    }

    public DataSourceJdbcRuntimeConfig getDataSourceJdbcRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return dataSourcesJdbcRuntimeConfig.jdbc;
        }

        DataSourceJdbcOuterNamedRuntimeConfig namedOuterConfig = dataSourcesJdbcRuntimeConfig.namedDataSources
                .get(dataSourceName);

        return namedOuterConfig != null ? namedOuterConfig.jdbc : new DataSourceJdbcRuntimeConfig();
    }

    public LegacyDataSourceJdbcBuildTimeConfig getLegacyDataSourceJdbcBuildTimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return legacyDataSourcesJdbcBuildTimeConfig.defaultDataSource;
        }

        LegacyDataSourceJdbcBuildTimeConfig namedConfig = legacyDataSourcesJdbcBuildTimeConfig.namedDataSources
                .get(dataSourceName);

        return namedConfig != null ? namedConfig : new LegacyDataSourceJdbcBuildTimeConfig();
    }

    public LegacyDataSourceRuntimeConfig getLegacyDataSourceRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return legacyDataSourcesRuntimeConfig.defaultDataSource;
        }

        LegacyDataSourceRuntimeConfig namedConfig = legacyDataSourcesRuntimeConfig.namedDataSources.get(dataSourceName);

        return namedConfig != null ? namedConfig : new LegacyDataSourceRuntimeConfig();
    }

    public LegacyDataSourceJdbcRuntimeConfig getLegacyDataSourceJdbcRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return legacyDataSourcesJdbcRuntimeConfig.defaultDataSource;
        }

        LegacyDataSourceJdbcRuntimeConfig namedConfig = legacyDataSourcesJdbcRuntimeConfig.namedDataSources
                .get(dataSourceName);

        return namedConfig != null ? namedConfig : new LegacyDataSourceJdbcRuntimeConfig();
    }

    private void checkConfigInjection() {
        if (dataSourcesBuildTimeConfig == null || dataSourcesJdbcBuildTimeConfig == null || dataSourcesRuntimeConfig == null
                || dataSourcesJdbcRuntimeConfig == null) {
            throw new IllegalStateException(
                    "The datasources are not ready to be consumed: the configuration has not been injected yet");
        }
    }

    /**
     * Uses the {@link ServiceLoader#load(Class) ServiceLoader to load the JDBC drivers} in context
     * of the current {@link Thread#getContextClassLoader() TCCL}
     */
    private static void loadDriversInTCCL() {
        // load JDBC drivers in the current TCCL
        final ServiceLoader<Driver> drivers = ServiceLoader.load(Driver.class);
        final Iterator<Driver> iterator = drivers.iterator();
        while (iterator.hasNext()) {
            try {
                // load the driver
                iterator.next();
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    @PreDestroy
    public void stop() {
        for (AgroalDataSource dataSource : dataSources) {
            if (dataSource != null) {
                dataSource.close();
            }
        }
    }
}
