package io.quarkus.agroal.runtime;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.inject.Singleton;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalPoolInterceptor;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration.ConnectionValidator;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.agroal.api.configuration.AgroalDataSourceConfiguration.DataSourceImplementation;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.api.transaction.TransactionIntegration;
import io.agroal.narayana.NarayanaTransactionIntegration;
import io.quarkus.agroal.DataSource;
import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig.DataSourceJdbcOuterNamedBuildTimeConfig;
import io.quarkus.agroal.runtime.DataSourcesJdbcRuntimeConfig.DataSourceJdbcOuterNamedRuntimeConfig;
import io.quarkus.agroal.runtime.JdbcDriver.JdbcDriverLiteral;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.credentials.runtime.CredentialsProviderFinder;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.datasource.runtime.LegacyDataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.LegacyDataSourcesRuntimeConfig;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * This class is sort of a producer for {@link AgroalDataSource}.
 * <p>
 * It isn't a CDI producer in the literal sense, but it created a synthetic bean
 * from {@code AgroalProcessor}
 * The {@code createDataSource} method is called at runtime (see
 * {@link AgroalRecorder#agroalDataSourceSupplier(String, DataSourcesRuntimeConfig)})
 * in order to produce the actual {@code AgroalDataSource} objects.
 */
@SuppressWarnings("deprecation")
@Singleton
public class DataSources {

    private static final Logger log = Logger.getLogger(DataSources.class.getName());

    private final DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig;
    private final DataSourcesRuntimeConfig dataSourcesRuntimeConfig;
    private final DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig;
    private final DataSourcesJdbcRuntimeConfig dataSourcesJdbcRuntimeConfig;
    private final LegacyDataSourcesJdbcBuildTimeConfig legacyDataSourcesJdbcBuildTimeConfig;
    private final LegacyDataSourcesRuntimeConfig legacyDataSourcesRuntimeConfig;
    private final LegacyDataSourcesJdbcRuntimeConfig legacyDataSourcesJdbcRuntimeConfig;
    private final TransactionManager transactionManager;
    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private final DataSourceSupport dataSourceSupport;
    private final Instance<AgroalPoolInterceptor> agroalPoolInterceptors;

    private final ConcurrentMap<String, AgroalDataSource> dataSources = new ConcurrentHashMap<>();

    public DataSources(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig, DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
            DataSourcesJdbcRuntimeConfig dataSourcesJdbcRuntimeConfig,
            LegacyDataSourcesJdbcBuildTimeConfig legacyDataSourcesJdbcBuildTimeConfig,
            LegacyDataSourcesRuntimeConfig legacyDataSourcesRuntimeConfig,
            LegacyDataSourcesJdbcRuntimeConfig legacyDataSourcesJdbcRuntimeConfig, TransactionManager transactionManager,
            TransactionSynchronizationRegistry transactionSynchronizationRegistry, DataSourceSupport dataSourceSupport,
            @Any Instance<AgroalPoolInterceptor> agroalPoolInterceptors) {
        this.dataSourcesBuildTimeConfig = dataSourcesBuildTimeConfig;
        this.dataSourcesRuntimeConfig = dataSourcesRuntimeConfig;
        this.dataSourcesJdbcBuildTimeConfig = dataSourcesJdbcBuildTimeConfig;
        this.dataSourcesJdbcRuntimeConfig = dataSourcesJdbcRuntimeConfig;
        this.legacyDataSourcesJdbcBuildTimeConfig = legacyDataSourcesJdbcBuildTimeConfig;
        this.legacyDataSourcesRuntimeConfig = legacyDataSourcesRuntimeConfig;
        this.legacyDataSourcesJdbcRuntimeConfig = legacyDataSourcesJdbcRuntimeConfig;
        this.transactionManager = transactionManager;
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        this.dataSourceSupport = dataSourceSupport;
        this.agroalPoolInterceptors = agroalPoolInterceptors;
    }

    /**
     * Meant to be used from recorders that create synthetic beans that need access to {@code Datasource}.
     * In such using {@code Arc.container.instance(DataSource.class)} is not possible because
     * {@code Datasource} is itself a synthetic bean.
     * <p>
     * This method relies on the fact that {@code DataSources} should - given the same input -
     * always return the same {@code AgroalDataSource} no matter how many times it is invoked
     * (which makes sense because {@code DataSource} is a {@code Singleton} bean).
     * <p>
     * This method is thread-safe
     */
    public static AgroalDataSource fromName(String dataSourceName) {
        return Arc.container().instance(DataSources.class).get()
                .getDataSource(dataSourceName);
    }

    public AgroalDataSource getDataSource(String dataSourceName) {
        return dataSources.computeIfAbsent(dataSourceName, new Function<String, AgroalDataSource>() {
            @Override
            public AgroalDataSource apply(String s) {
                return doCreateDataSource(s);
            }
        });
    }

    public AgroalDataSource doCreateDataSource(String dataSourceName) {
        if (!dataSourceSupport.entries.containsKey(dataSourceName)) {
            throw new IllegalArgumentException("No datasource named '" + dataSourceName + "' exists");
        }

        DataSourceJdbcBuildTimeConfig dataSourceJdbcBuildTimeConfig = getDataSourceJdbcBuildTimeConfig(dataSourceName);
        DataSourceRuntimeConfig dataSourceRuntimeConfig = getDataSourceRuntimeConfig(dataSourceName);
        DataSourceJdbcRuntimeConfig dataSourceJdbcRuntimeConfig = getDataSourceJdbcRuntimeConfig(dataSourceName);
        LegacyDataSourceJdbcBuildTimeConfig legacyDataSourceJdbcBuildTimeConfig = getLegacyDataSourceJdbcBuildTimeConfig(
                dataSourceName);
        LegacyDataSourceRuntimeConfig legacyDataSourceRuntimeConfig = getLegacyDataSourceRuntimeConfig(dataSourceName);
        LegacyDataSourceJdbcRuntimeConfig legacyDataSourceJdbcRuntimeConfig = getLegacyDataSourceJdbcRuntimeConfig(
                dataSourceName);

        DataSourceSupport.Entry matchingSupportEntry = dataSourceSupport.entries.get(dataSourceName);
        boolean isLegacy = matchingSupportEntry.isLegacy;
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

        String resolvedDriverClass = matchingSupportEntry.resolvedDriverClass;
        Class<?> driver;
        try {
            driver = Class.forName(resolvedDriverClass, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Unable to load the datasource driver " + resolvedDriverClass + " for datasource " + dataSourceName, e);
        }

        String resolvedDbKind = matchingSupportEntry.resolvedDbKind;
        InstanceHandle<AgroalConnectionConfigurer> agroalConnectionConfigurerHandle = Arc.container().instance(
                AgroalConnectionConfigurer.class,
                new JdbcDriverLiteral(resolvedDbKind));

        AgroalDataSourceConfigurationSupplier dataSourceConfiguration = new AgroalDataSourceConfigurationSupplier();

        // Set pool-less mode
        if (!dataSourceJdbcRuntimeConfig.poolingEnabled) {
            dataSourceConfiguration.dataSourceImplementation(DataSourceImplementation.AGROAL_POOLLESS);
        }

        AgroalConnectionPoolConfigurationSupplier poolConfiguration = dataSourceConfiguration.connectionPoolConfiguration();
        AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration = poolConfiguration
                .connectionFactoryConfiguration();

        boolean mpMetricsPresent = dataSourceSupport.mpMetricsPresent;
        if (!isLegacy) {
            applyNewConfiguration(dataSourceConfiguration, poolConfiguration, connectionFactoryConfiguration, driver,
                    dataSourceJdbcBuildTimeConfig, dataSourceRuntimeConfig, dataSourceJdbcRuntimeConfig, mpMetricsPresent);
        } else {
            applyLegacyConfiguration(dataSourceConfiguration, poolConfiguration, connectionFactoryConfiguration, driver,
                    dataSourceRuntimeConfig, legacyDataSourceJdbcBuildTimeConfig, legacyDataSourceRuntimeConfig,
                    legacyDataSourceJdbcRuntimeConfig, mpMetricsPresent);
        }

        if (dataSourceSupport.disableSslSupport) {
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

        // Set pool interceptors for this datasource
        Collection<AgroalPoolInterceptor> interceptorList = agroalPoolInterceptors
                .select(dataSourceName == null || DataSourceUtil.isDefault(dataSourceName)
                        ? Default.Literal.INSTANCE
                        : new DataSource.DataSourceLiteral(dataSourceName))
                .stream().collect(Collectors.toList());
        if (!interceptorList.isEmpty()) {
            dataSource.setPoolInterceptors(interceptorList);
        }

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

        // credentials provider
        if (dataSourceRuntimeConfig.credentialsProvider.isPresent()) {
            String beanName = dataSourceRuntimeConfig.credentialsProviderName.orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);
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

        // credentials provider
        if (dataSourceRuntimeConfig.credentialsProvider.isPresent()) {
            String beanName = dataSourceRuntimeConfig.credentialsProviderName.orElse(null);
            CredentialsProvider credentialsProvider = CredentialsProviderFinder.find(beanName);

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
        for (AgroalDataSource dataSource : dataSources.values()) {
            if (dataSource != null) {
                dataSource.close();
            }
        }
    }
}
