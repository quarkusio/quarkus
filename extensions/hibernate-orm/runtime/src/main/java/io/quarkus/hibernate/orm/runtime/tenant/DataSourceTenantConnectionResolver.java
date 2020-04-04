package io.quarkus.hibernate.orm.runtime.tenant;

import javax.enterprise.inject.Instance;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource.DataSourceLiteral;
import io.quarkus.arc.DefaultBean;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusConnectionProvider;

/**
 * Creates a database connection based on the data sources in the configuration file.
 * The tenant identifier is used as the data source name.
 * 
 * @author Michael Schnell
 *
 */
@DefaultBean
public class DataSourceTenantConnectionResolver implements TenantConnectionResolver {

    Instance<AgroalDataSource> dataSourceInstance;

    @Override
    public ConnectionProvider resolve(String tenantId) {
        final Instance<AgroalDataSource> instance;
        if (tenantId == null) {
            instance = dataSourceInstance.select();
        } else {
            instance = dataSourceInstance.select(new DataSourceLiteral(tenantId));
        }
        if (instance.isUnsatisfied()) {
            throw new IllegalStateException("No instance of datasource found for tenant: " + tenantId);
        }
        AgroalDataSource dataSource = instance.get();
        return new QuarkusConnectionProvider(dataSource);
    }

}
