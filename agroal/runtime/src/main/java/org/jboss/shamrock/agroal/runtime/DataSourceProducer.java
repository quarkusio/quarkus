/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.shamrock.agroal.runtime;

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

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.api.transaction.TransactionIntegration;
import io.agroal.narayana.NarayanaTransactionIntegration;

@ApplicationScoped
public class DataSourceProducer {

    private static final Logger log = Logger.getLogger(DataSourceProducer.class.getName());

    private Class driver;
    private String dataSourceName;
    private String url;
    private String userName;
    private String password;
    private boolean jta = true;
    private boolean connectable;
    private boolean xa;

    private AgroalDataSource agroalDataSource;

    @Inject
    private TransactionManager transactionManager;

    @Inject
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    private AgroalDataSource dataSource;

    @Produces
    @ApplicationScoped
    public AgroalDataSource getDatasource() throws SQLException {
        Class<?> providerClass = driver;
        if (xa) {
            if (!XADataSource.class.isAssignableFrom(providerClass)) {
                throw new RuntimeException("Driver is not an XA datasource and xa has been configured");
            }
        } else {
            if (providerClass != null && !DataSource.class.isAssignableFrom(providerClass) && !Driver.class.isAssignableFrom(providerClass)) {
                throw new RuntimeException("Driver is an XA datasource and xa has been configured");
            }
        }
        AgroalDataSourceConfigurationSupplier dataSourceConfiguration = new AgroalDataSourceConfigurationSupplier();
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl(url);
        dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().connectionProviderClass(providerClass);

        if (jta || xa) {
            try {
                arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier("shamrock");
            } catch (CoreEnvironmentBeanException e) {
                e.printStackTrace();
            }
            TransactionIntegration txIntegration = new NarayanaTransactionIntegration(transactionManager, transactionSynchronizationRegistry, null, connectable);
            dataSourceConfiguration.connectionPoolConfiguration().transactionIntegration(txIntegration);
        }
        // use the name / password from the callbacks
        if (userName != null) {
            dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().principal(new NamePrincipal(userName));
        }
        if (password != null) {
            dataSourceConfiguration.connectionPoolConfiguration().connectionFactoryConfiguration().credential(new SimplePassword(password));
        }

        agroalDataSource = AgroalDataSource.from(dataSourceConfiguration);
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

    public Class getDriver() {
        return driver;
    }

    public void setDriver(Class driver) {
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
}
