package io.quarkus.hibernate.orm.runtime.tenant;

import java.sql.Connection;
import java.sql.SQLException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalDataSourceConfiguration;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.DefaultBean;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusConnectionProvider;

/**
 * Creates a database connection based on the data sources in the configuration file.
 * The tenant identifier is used as the data source name.
 * 
 * @author Michael Schnell
 *
 */
@DefaultBean
@ApplicationScoped
public class DataSourceTenantConnectionResolver implements TenantConnectionResolver {

    private static final Logger LOG = Logger.getLogger(DataSourceTenantConnectionResolver.class);

    @Inject
    JPAConfig jpaConfig;

    @Override
    public ConnectionProvider resolve(String tenantId) {

        LOG.debugv("resolve({0})", tenantId);

        final MultiTenancyStrategy strategy = jpaConfig.getMultiTenancyStrategy();
        LOG.debugv("multitenancy strategy: {0}", strategy);
        AgroalDataSource dataSource = tenantDataSource(jpaConfig, tenantId, strategy);
        if (dataSource == null) {
            throw new IllegalStateException("No instance of datasource found for tenant: " + tenantId);
        }
        if (strategy == MultiTenancyStrategy.SCHEMA) {
            return new TenantConnectionProvider(tenantId, dataSource);
        }
        return new QuarkusConnectionProvider(dataSource);
    }

    /**
     * Create a new data source from the given configuration.
     * 
     * @param config Configuration to use.
     * 
     * @return New data source instance.
     */
    private static AgroalDataSource createFrom(AgroalDataSourceConfiguration config) {
        try {
            return AgroalDataSource.from(config);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create a new data source based on the default config", ex);
        }
    }

    /**
     * Returns either the default data source or the tenant specific one.
     * 
     * @param tenantId Tenant identifier. The value is required (non-{@literal null}) in case of
     *        {@link MultiTenancyStrategy#DATABASE}.
     * @param strategy Current multitenancy strategy Required value that cannot be {@literal null}.
     * 
     * @return Data source.
     */
    private static AgroalDataSource tenantDataSource(JPAConfig jpaConfig, String tenantId, MultiTenancyStrategy strategy) {
        if (strategy != MultiTenancyStrategy.SCHEMA) {
            return Arc.container().instance(AgroalDataSource.class, new DataSource.DataSourceLiteral(tenantId)).get();
        }
        String dataSourceName = jpaConfig.getMultiTenancySchemaDataSource();
        if (dataSourceName == null) {
            AgroalDataSource dataSource = Arc.container().instance(AgroalDataSource.class).get();
            return createFrom(dataSource.getConfiguration());
        }
        return Arc.container().instance(AgroalDataSource.class, new DataSource.DataSourceLiteral(dataSourceName)).get();
    }

    private static class TenantConnectionProvider extends QuarkusConnectionProvider {

        private final String tenantId;

        public TenantConnectionProvider(String tenantId, AgroalDataSource dataSource) {
            super(dataSource);
            this.tenantId = tenantId;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection conn = super.getConnection();
            conn.setSchema(tenantId);
            LOG.debugv("Set tenant {0} for connection: {1}", tenantId, conn);
            return conn;
        }

    }

}
