/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.agroal.runtime;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.api.transaction.TransactionIntegration;
import io.agroal.narayana.NarayanaTransactionIntegration;

public abstract class AbstractDataSourceProducer {

    private static final Logger log = Logger.getLogger(AbstractDataSourceProducer.class.getName());

    private AgroalBuildTimeConfig buildTimeConfig;
    private AgroalRuntimeConfig runtimeConfig;
    private boolean disableSslSupport = false;

    private List<AgroalDataSource> dataSources = new ArrayList<>();

    @Inject
    TransactionManager transactionManager;

    @Inject
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    protected DataSourceBuildTimeConfig getDefaultBuildTimeConfig() {
        return buildTimeConfig.defaultDataSource;
    }

    protected Optional<DataSourceRuntimeConfig> getDefaultRuntimeConfig() {
        checkRuntimeConfig();

        return Optional.of(runtimeConfig.defaultDataSource);
    }

    protected DataSourceBuildTimeConfig getBuildTimeConfig(String dataSourceName) {
        return buildTimeConfig.namedDataSources.get(dataSourceName);
    }

    protected Optional<DataSourceRuntimeConfig> getRuntimeConfig(String dataSourceName) {
        checkRuntimeConfig();

        return Optional.ofNullable(runtimeConfig.namedDataSources.get(dataSourceName));
    }

    protected AgroalDataSource createDataSource(String dataSourceName,
            DataSourceBuildTimeConfig dataSourceBuildTimeConfig,
            Optional<DataSourceRuntimeConfig> dataSourceRuntimeConfigOptional) throws SQLException {
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

        if (dataSourceBuildTimeConfig.xa) {
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
        poolConfiguration.connectionFactoryConfiguration().jdbcUrl(url);
        poolConfiguration.connectionFactoryConfiguration().connectionProviderClass(driver);

        if (dataSourceBuildTimeConfig.jta || dataSourceBuildTimeConfig.xa) {
            TransactionIntegration txIntegration = new NarayanaTransactionIntegration(transactionManager,
                    transactionSynchronizationRegistry);
            poolConfiguration.transactionIntegration(txIntegration);
        }

        // Authentication
        if (dataSourceRuntimeConfig.username.isPresent()) {
            poolConfiguration.connectionFactoryConfiguration()
                    .principal(new NamePrincipal(dataSourceRuntimeConfig.username.get()));
        }
        if (dataSourceRuntimeConfig.password.isPresent()) {
            poolConfiguration.connectionFactoryConfiguration()
                    .credential(new SimplePassword(dataSourceRuntimeConfig.password.get()));
        }

        // Pool size configuration:
        poolConfiguration.minSize(dataSourceRuntimeConfig.minSize);
        poolConfiguration.maxSize(dataSourceRuntimeConfig.maxSize);
        if (dataSourceRuntimeConfig.initialSize.isPresent() && dataSourceRuntimeConfig.initialSize.get() > 0) {
            poolConfiguration.initialSize(dataSourceRuntimeConfig.initialSize.get());
        }

        // Connection management
        if (dataSourceRuntimeConfig.acquisitionTimeout.isPresent()) {
            poolConfiguration.acquisitionTimeout(dataSourceRuntimeConfig.acquisitionTimeout.get());
        }
        if (dataSourceRuntimeConfig.backgroundValidationInterval.isPresent()) {
            poolConfiguration.validationTimeout(dataSourceRuntimeConfig.backgroundValidationInterval.get());
        }
        if (dataSourceRuntimeConfig.idleRemovalInterval.isPresent()) {
            poolConfiguration.reapTimeout(dataSourceRuntimeConfig.idleRemovalInterval.get());
        }
        if (dataSourceRuntimeConfig.leakDetectionInterval.isPresent()) {
            poolConfiguration.leakTimeout(dataSourceRuntimeConfig.leakDetectionInterval.get());
        }

        // SSL support: we should push the driver specific code to the driver extensions but it will have to do for now
        if (disableSslSupport) {
            switch (driverName) {
                case "org.postgresql.Driver":
                    poolConfiguration.connectionFactoryConfiguration().jdbcProperty("sslmode", "disable");
                    break;
                case "com.mysql.jdbc.Driver":
                    poolConfiguration.connectionFactoryConfiguration().jdbcProperty("useSSL", "false");
                    break;
                default:
                    log.warn("Agroal does not support disabling SSL for driver " + driverName);
            }
        }

        // Explicit reference to bypass reflection need of the ServiceLoader used by AgroalDataSource#from
        AgroalDataSource dataSource = new io.agroal.pool.DataSource(dataSourceConfiguration.get());

        log.debug("Started data source " + dataSourceName + " connected to " + url);

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
            throw new IllegalStateException("The datasources are not ready to be consumed: the runtime configuration has not been injected yet");
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
