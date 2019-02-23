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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.api.transaction.TransactionIntegration;
import io.agroal.narayana.NarayanaTransactionIntegration;

@ApplicationScoped
public class DataSourceProducer {

    private static final Logger log = Logger.getLogger(DataSourceProducer.class.getName());

    private static final int DEFAULT_MIN_POOL_SIZE = 2;
    private static final int DEFAULT_MAX_POOL_SIZE = 20;

    private Class<?> driver;
    private String dataSourceName;
    private String url;
    private String userName;
    private String password;
    private boolean jta = true;
    private boolean connectable;
    private boolean xa;
    private Integer minSize;
    private Integer maxSize;
    private boolean disableSslSupport = false;

    private AgroalDataSource agroalDataSource;

    @Inject
    TransactionManager transactionManager;

    @Inject
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @Produces
    @ApplicationScoped
    public AgroalDataSource getDatasource() throws SQLException {
        Class<?> providerClass = driver;
        if (xa) {
            if (!XADataSource.class.isAssignableFrom(providerClass)) {
                throw new RuntimeException("Driver is not an XA datasource and xa has been configured");
            }
        } else {
            if (providerClass != null && !DataSource.class.isAssignableFrom(providerClass)
                    && !Driver.class.isAssignableFrom(providerClass)) {
                throw new RuntimeException("Driver is an XA datasource and xa has been configured");
            }
        }

        String targetUrl = System.getenv("DATASOURCE_URL");
        if (targetUrl == null || targetUrl.isEmpty()) {
            targetUrl = url;
        }

        AgroalDataSourceConfigurationSupplier dataSourceConfiguration = new AgroalDataSourceConfigurationSupplier();
        final AgroalConnectionPoolConfigurationSupplier poolConfiguration = dataSourceConfiguration
                .connectionPoolConfiguration();
        poolConfiguration.connectionFactoryConfiguration().jdbcUrl(targetUrl);
        poolConfiguration.connectionFactoryConfiguration().connectionProviderClass(providerClass);

        if (jta || xa) {
            TransactionIntegration txIntegration = new NarayanaTransactionIntegration(transactionManager,
                    transactionSynchronizationRegistry, null, connectable);
            poolConfiguration.transactionIntegration(txIntegration);
        }
        // use the name / password from the callbacks
        if (userName != null) {
            poolConfiguration
                    .connectionFactoryConfiguration().principal(new NamePrincipal(userName));
        }
        if (password != null) {
            poolConfiguration
                    .connectionFactoryConfiguration().credential(new SimplePassword(password));
        }

        //Pool size configuration:
        if (minSize != null) {
            poolConfiguration.minSize(minSize);
        } else {
            log.warning("Agroal pool 'minSize' was not set: setting to default value " + DEFAULT_MIN_POOL_SIZE);
            poolConfiguration.minSize(DEFAULT_MIN_POOL_SIZE);
        }
        if (maxSize != null) {
            poolConfiguration.maxSize(maxSize);
        } else {
            log.warning("Agroal pool 'maxSize' was not set: setting to default value " + DEFAULT_MAX_POOL_SIZE);
            poolConfiguration.maxSize(DEFAULT_MAX_POOL_SIZE);
        }

        // SSL support: we should push the driver specific code to the driver extensions but it will have to do for now
        if (disableSslSupport) {
            switch (driver.getName()) {
                case "org.postgresql.Driver":
                    poolConfiguration.connectionFactoryConfiguration().jdbcProperty("sslmode", "disable");
                    break;
                case "com.mysql.jdbc.Driver":
                    poolConfiguration.connectionFactoryConfiguration().jdbcProperty("useSSL", "false");
                    break;
                default:
                    log.warning("Agroal does not support disabling SSL for driver " + driver.getName());
            }
        }

        //Explicit reference to bypass reflection need of the ServiceLoader used by AgroalDataSource#from
        agroalDataSource = new io.agroal.pool.DataSource(dataSourceConfiguration.get());
        log.log(Level.INFO, "Started data source " + url);
        return agroalDataSource;
    }

    @PreDestroy
    public void stop() {
        if (agroalDataSource != null) {
            agroalDataSource.close();
        }
    }

    public static Logger getLog() {
        return log;
    }

    public Class<?> getDriver() {
        return driver;
    }

    public void setDriver(Class<?> driver) {
        this.driver = driver;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isJta() {
        return jta;
    }

    public void setJta(boolean jta) {
        this.jta = jta;
    }

    public boolean isConnectable() {
        return connectable;
    }

    public void setConnectable(boolean connectable) {
        this.connectable = connectable;
    }

    public boolean isXa() {
        return xa;
    }

    public void setXa(boolean xa) {
        this.xa = xa;
    }

    public AgroalDataSource getAgroalDataSource() {
        return agroalDataSource;
    }

    public void setAgroalDataSource(AgroalDataSource agroalDataSource) {
        this.agroalDataSource = agroalDataSource;
    }

    public Integer getMinSize() {
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        this.minSize = minSize;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    public void disableSslSupport() {
        this.disableSslSupport = true;
    }
}
