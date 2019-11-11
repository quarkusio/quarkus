package io.quarkus.agroal.runtime;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceListener;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration.ConnectionValidator;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.api.transaction.TransactionIntegration;
import io.agroal.narayana.NarayanaTransactionIntegration;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.vault.CredentialsProvider;

public abstract class AbstractDataSourceProducer {

    private static final Logger log = Logger.getLogger(AbstractDataSourceProducer.class.getName());

    private AgroalBuildTimeConfig buildTimeConfig;
    private AgroalRuntimeConfig runtimeConfig;
    private boolean disableSslSupport = false;

    private List<AgroalDataSource> dataSources = new ArrayList<>();

    @Inject
    public TransactionManager transactionManager;

    @Inject
    public TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @Inject
    public BeanManager beanManager;

    public DataSourceBuildTimeConfig getDefaultBuildTimeConfig() {
        return buildTimeConfig.defaultDataSource;
    }

    public Optional<DataSourceRuntimeConfig> getDefaultRuntimeConfig() {
        checkRuntimeConfig();

        return Optional.of(runtimeConfig.defaultDataSource);
    }

    public DataSourceBuildTimeConfig getBuildTimeConfig(String dataSourceName) {
        return buildTimeConfig.namedDataSources.get(dataSourceName);
    }

    public Optional<DataSourceRuntimeConfig> getRuntimeConfig(String dataSourceName) {
        checkRuntimeConfig();

        return Optional.ofNullable(runtimeConfig.namedDataSources.get(dataSourceName));
    }

    public AgroalDataSource createDataSource(String dataSourceName,
            DataSourceBuildTimeConfig dataSourceBuildTimeConfig,
            Optional<DataSourceRuntimeConfig> dataSourceRuntimeConfigOptional) {
        if (!dataSourceRuntimeConfigOptional.isPresent() || !dataSourceRuntimeConfigOptional.get().url.isPresent()) {
            log.warn("Datasource " + dataSourceName + " not started: driver and/or url are not defined.");
            return null;
        }

        DataSourceRuntimeConfig dataSourceRuntimeConfig = dataSourceRuntimeConfigOptional.get();

        String driverName = dataSourceBuildTimeConfig.driver.get();
        Class<?> driver;
        try {
            driver = Class.forName(driverName, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load the dataSource driver", e);
        }

        String url = dataSourceRuntimeConfig.url.get();

        //TODO should we do such checks at build time only? All these are currently defined at build - but it could change
        //depending on if and how we could do Driver auto-detection.
        final io.quarkus.agroal.runtime.TransactionIntegration transactionIntegration = dataSourceBuildTimeConfig.transactions;
        if (transactionIntegration == io.quarkus.agroal.runtime.TransactionIntegration.XA) {
            if (!XADataSource.class.isAssignableFrom(driver)) {
                throw new RuntimeException("Driver is not an XA dataSource and XA has been configured");
            }
        } else {
            if (driver != null && !DataSource.class.isAssignableFrom(driver) && !Driver.class.isAssignableFrom(driver)) {
                throw new RuntimeException("Driver is an XA dataSource and XA has not been configured");
            }
        }

        AgroalDataSourceConfigurationSupplier dataSourceConfiguration = new AgroalDataSourceConfigurationSupplier();

        AgroalConnectionPoolConfigurationSupplier poolConfiguration = dataSourceConfiguration.connectionPoolConfiguration();
        AgroalConnectionFactoryConfigurationSupplier agroalConnectionFactoryConfigurationSupplier = poolConfiguration
                .connectionFactoryConfiguration();
        agroalConnectionFactoryConfigurationSupplier.jdbcUrl(url);
        agroalConnectionFactoryConfigurationSupplier.connectionProviderClass(driver);
        agroalConnectionFactoryConfigurationSupplier.trackJdbcResources(dataSourceRuntimeConfig.detectStatementLeaks);

        if (dataSourceRuntimeConfig.transactionIsolationLevel.isPresent()) {
            agroalConnectionFactoryConfigurationSupplier
                    .jdbcTransactionIsolation(
                            dataSourceRuntimeConfig.transactionIsolationLevel.get());
        }

        if (transactionIntegration != io.quarkus.agroal.runtime.TransactionIntegration.DISABLED) {
            TransactionIntegration txIntegration = new NarayanaTransactionIntegration(transactionManager,
                    transactionSynchronizationRegistry);
            poolConfiguration.transactionIntegration(txIntegration);
        }

        // New connection SQL
        if (dataSourceRuntimeConfig.newConnectionSql.isPresent()) {
            agroalConnectionFactoryConfigurationSupplier.initialSql(dataSourceRuntimeConfig.newConnectionSql.get());
        }

        // metrics
        dataSourceConfiguration.metricsEnabled(dataSourceRuntimeConfig.enableMetrics);

        // Authentication
        if (dataSourceRuntimeConfig.username.isPresent()) {
            agroalConnectionFactoryConfigurationSupplier
                    .principal(new NamePrincipal(dataSourceRuntimeConfig.username.get()));
        }
        if (dataSourceRuntimeConfig.password.isPresent()) {
            agroalConnectionFactoryConfigurationSupplier
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
            agroalConnectionFactoryConfigurationSupplier
                    .credential(new AgroalVaultCredentialsProviderPassword(name, credentialsProvider));
        }

        // Pool size configuration:
        poolConfiguration.minSize(dataSourceRuntimeConfig.minSize);
        poolConfiguration.maxSize(dataSourceRuntimeConfig.maxSize);
        if (dataSourceRuntimeConfig.initialSize.isPresent() && dataSourceRuntimeConfig.initialSize.get() > 0) {
            poolConfiguration.initialSize(dataSourceRuntimeConfig.initialSize.get());
        }

        // Connection management
        poolConfiguration.connectionValidator(ConnectionValidator.defaultValidator());
        if (dataSourceRuntimeConfig.acquisitionTimeout.isPresent()) {
            poolConfiguration.acquisitionTimeout(dataSourceRuntimeConfig.acquisitionTimeout.get());
        }
        if (dataSourceRuntimeConfig.backgroundValidationInterval.isPresent()) {
            poolConfiguration.validationTimeout(dataSourceRuntimeConfig.backgroundValidationInterval.get());
        }
        if (dataSourceRuntimeConfig.validationQuerySql.isPresent()) {
            String validationQuery = dataSourceRuntimeConfig.validationQuerySql.get();
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
        if (dataSourceRuntimeConfig.idleRemovalInterval.isPresent()) {
            poolConfiguration.reapTimeout(dataSourceRuntimeConfig.idleRemovalInterval.get());
        }
        if (dataSourceRuntimeConfig.leakDetectionInterval.isPresent()) {
            poolConfiguration.leakTimeout(dataSourceRuntimeConfig.leakDetectionInterval.get());
        }
        if (dataSourceRuntimeConfig.maxLifetime.isPresent()) {
            poolConfiguration.maxLifetime(dataSourceRuntimeConfig.maxLifetime.get());
        }

        // SSL support: we should push the driver specific code to the driver extensions but it will have to do for now
        if (disableSslSupport) {
            switch (driverName) {
                case "org.postgresql.Driver":
                    agroalConnectionFactoryConfigurationSupplier.jdbcProperty("sslmode", "disable");
                    break;
                case "org.mariadb.jdbc.Driver":
                    agroalConnectionFactoryConfigurationSupplier.jdbcProperty("useSSL", "false");
                    break;
                default:
                    log.warn("Agroal does not support disabling SSL for driver " + driverName);
            }
        }

        // Identify AgroalDataSourceListener beans and filter appropriately to this dataSourceName.
        List<PrioritizedDataSourceListenerWrapper> listeners = beanManager.getBeans(AgroalDataSourceListener.class).stream()
                .map(beanDef -> new PrioritizedDataSourceListenerWrapper((Bean<AgroalDataSourceListener>) beanDef))
                .filter(pds -> pds.appliesTo(dataSourceName))
                .collect(Collectors.toList());

        // Add the default event logging listener..
        listeners.add(new PrioritizedDataSourceListenerWrapper(
                new AgroalEventLoggingListener(dataSourceName), 0, dataSourceName));

        // Explicit reference to bypass reflection need of the ServiceLoader used by AgroalDataSource#from
        AgroalDataSource dataSource = new io.agroal.pool.DataSource(dataSourceConfiguration.get(),
                listeners.stream().sorted().map(listenersWrapper -> listenersWrapper.listener)
                        .toArray(size -> new AgroalDataSourceListener[size]));

        log.debugv("Started data source {0} connected to {1}", dataSource, url);

        this.dataSources.add(dataSource);

        return dataSource;
    }

    public void setBuildTimeConfig(AgroalBuildTimeConfig buildTimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
    }

    public void setRuntimeConfig(AgroalRuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void disableSslSupport() {
        this.disableSslSupport = true;
    }

    private void checkRuntimeConfig() {
        if (runtimeConfig == null) {
            throw new IllegalStateException(
                    "The datasources are not ready to be consumed: the runtime configuration has not been injected yet");
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
    
    /**
     * Wrapper to facilitate filtering & sorting of applicable CDI produced AgroalDataSourceListeners
     */
    private class PrioritizedDataSourceListenerWrapper implements Comparable<PrioritizedDataSourceListenerWrapper> {
        final AgroalDataSourceListener listener;
        final int priority;
        final String dataSource;

        PrioritizedDataSourceListenerWrapper(final AgroalDataSourceListener listener, final int priority,
                final String dataSourceName) {
            this.listener = listener;
            this.priority = priority;
            this.dataSource = dataSourceName;
        }

        PrioritizedDataSourceListenerWrapper(final Bean<AgroalDataSourceListener> listener) {
            this.listener = (AgroalDataSourceListener) beanManager.getReference(listener, listener.getBeanClass(),
                    beanManager.createCreationalContext(listener));

            Priority p = listener.getBeanClass().getAnnotation(Priority.class);
            this.priority = p != null ? p.value() : Interceptor.Priority.APPLICATION;

            io.quarkus.agroal.DataSource ds = listener.getBeanClass().getAnnotation(io.quarkus.agroal.DataSource.class);
            this.dataSource = ds != null ? ds.value() : null;
        }

        boolean appliesTo(final String dataSourceNamed) {
            return dataSource == null || dataSource.equals(dataSourceNamed);
        }

        @Override
        public int compareTo(final PrioritizedDataSourceListenerWrapper o) {
            return Integer.compare(this.priority, o.priority);
        }
    }
}
