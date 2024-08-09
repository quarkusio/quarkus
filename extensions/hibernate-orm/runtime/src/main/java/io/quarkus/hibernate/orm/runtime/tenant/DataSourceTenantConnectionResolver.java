package io.quarkus.hibernate.orm.runtime.tenant;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.agroal.runtime.AgroalDataSourceUtil;
import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusConnectionProvider;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;

/**
 * Creates a database connection based on the data sources in the configuration file.
 * The tenant identifier is used as the data source name.
 *
 * @author Michael Schnell
 *
 */
public class DataSourceTenantConnectionResolver implements TenantConnectionResolver {

    private static final Logger LOG = Logger.getLogger(DataSourceTenantConnectionResolver.class);

    private String persistenceUnitName;

    private Optional<String> dataSourceName;

    private MultiTenancyStrategy multiTenancyStrategy;

    public DataSourceTenantConnectionResolver() {
    }

    public DataSourceTenantConnectionResolver(String persistenceUnitName, Optional<String> dataSourceName,
            MultiTenancyStrategy multiTenancyStrategy) {
        this.persistenceUnitName = persistenceUnitName;
        this.dataSourceName = dataSourceName;
        this.multiTenancyStrategy = multiTenancyStrategy;
    }

    @Override
    public ConnectionProvider resolve(String tenantId) {
        LOG.debugv("resolve((persistenceUnitName={0}, tenantIdentifier={1})", persistenceUnitName, tenantId);
        LOG.debugv("multitenancy strategy: {0}", multiTenancyStrategy);

        AgroalDataSource dataSource = tenantDataSource(dataSourceName, tenantId, multiTenancyStrategy);
        if (dataSource == null) {
            throw new IllegalStateException(
                    String.format(Locale.ROOT, "No instance of datasource found for persistence unit '%1$s' and tenant '%2$s'",
                            persistenceUnitName, tenantId));
        }
        return switch (multiTenancyStrategy) {
            case DATABASE -> new QuarkusConnectionProvider(dataSource);
            case SCHEMA -> new SchemaTenantConnectionProvider(tenantId, dataSource);
            default -> throw new IllegalStateException("Unexpected multitenancy strategy: " + multiTenancyStrategy);
        };
    }

    private static AgroalDataSource tenantDataSource(Optional<String> dataSourceName, String tenantId,
            MultiTenancyStrategy strategy) {
        return switch (strategy) {
            case DATABASE -> Arc.container().instance(AgroalDataSource.class, new DataSource.DataSourceLiteral(tenantId)).get();
            // The datasource name should always be present when using a multi-tenancy other than DATABASE;
            // we perform checks in HibernateOrmProcessor during the build.
            case SCHEMA -> getDataSource(dataSourceName.get());
            default -> throw new IllegalStateException("Unexpected multitenancy strategy: " + strategy);
        };
    }

    private static AgroalDataSource getDataSource(String dataSourceName) {
        return Arc.container().instance(AgroalDataSource.class, AgroalDataSourceUtil.qualifier(dataSourceName)).get();
    }

    private static class SchemaTenantConnectionProvider extends QuarkusConnectionProvider {

        private final String tenantId;

        public SchemaTenantConnectionProvider(String tenantId, AgroalDataSource dataSource) {
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
