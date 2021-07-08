package io.quarkus.it.hibernate.multitenancy;

import java.io.Closeable;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.Vetoed;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusConnectionProvider;
import io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver;

@Vetoed
class CustomTenantConnectionResolver implements Closeable, TenantConnectionResolver {

    private static final Logger LOG = Logger.getLogger(CustomTenantConnectionResolver.class);

    private final ConnectionConfig config;
    private final String puName;

    private final ConcurrentHashMap<String, AgroalDataSource> dataSources = new ConcurrentHashMap<>();

    CustomTenantConnectionResolver(ConnectionConfig config, String puName) {
        this.config = config;
        this.puName = puName;
    }

    @Override
    public ConnectionProvider resolve(String tenantId) {
        return new QuarkusConnectionProvider(getTenantDataSource(tenantId));
    }

    @Override
    public void close() {
        for (Map.Entry<String, AgroalDataSource> entry : dataSources.entrySet()) {
            try {
                entry.getValue().close();
            } catch (RuntimeException e) {
                LOG.errorf(e, "Could not close datasource %s", entry.getKey());
            }
        }
    }

    private AgroalDataSource getTenantDataSource(String tenantId) {
        return dataSources.computeIfAbsent(tenantId, this::createDataSource);
    }

    private AgroalDataSource createDataSource(String tenantId) {
        try {
            AgroalDataSourceConfigurationSupplier configurationSupplier = new AgroalDataSourceConfigurationSupplier();
            AgroalConnectionPoolConfigurationSupplier connectionPoolConfig = configurationSupplier
                    .connectionPoolConfiguration();
            AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfig = connectionPoolConfig
                    .connectionFactoryConfiguration();

            ConnectionConfig.PuConfig puConfig = config.pu().get(puName);

            connectionFactoryConfig.jdbcUrl(config.urlPrefix() + "/" + tenantId);
            connectionPoolConfig.maxSize(puConfig.maxPoolSizePerTenant());

            connectionFactoryConfig.principal(new NamePrincipal(puConfig.username()));
            connectionFactoryConfig.credential(new SimplePassword(puConfig.password()));

            return AgroalDataSource.from(configurationSupplier.get());
        } catch (SQLException | RuntimeException e) {
            throw new IllegalStateException("Exception while creating datasource for tenant " + tenantId, e);
        }
    }
}
