package io.quarkus.quartz.runtime;

import java.sql.Connection;
import java.sql.SQLException;

import javax.enterprise.util.AnnotationLiteral;
import javax.sql.DataSource;

import org.quartz.utils.PoolingConnectionProvider;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

public class QuarkusQuartzConnectionPoolProvider implements PoolingConnectionProvider {
    private AgroalDataSource dataSource;
    private static String dataSourceName;

    public QuarkusQuartzConnectionPoolProvider() {
        final ArcContainer container = Arc.container();
        final InstanceHandle<AgroalDataSource> instanceHandle;
        final boolean useDefaultDataSource = "QUARKUS_QUARTZ_DEFAULT_DATASOURCE".equals(dataSourceName);
        if (useDefaultDataSource) {
            instanceHandle = container.instance(AgroalDataSource.class);
        } else {
            instanceHandle = container.instance(AgroalDataSource.class, new DataSourceLiteral(dataSourceName));
        }
        if (instanceHandle.isAvailable()) {
            this.dataSource = instanceHandle.get();
        } else {
            String message = String.format(
                    "JDBC Store configured but '%s' datasource is missing. You can configure your datasource by following the guide available at: https://quarkus.io/guides/datasource",
                    useDefaultDataSource ? "default" : dataSourceName);
            throw new IllegalStateException(message);
        }
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void shutdown() {
        // Do nothing as the connection will be closed inside the Agroal extension
    }

    @Override
    public void initialize() {

    }

    static void setDataSourceName(String dataSourceName) {
        QuarkusQuartzConnectionPoolProvider.dataSourceName = dataSourceName;
    }

    private static class DataSourceLiteral extends AnnotationLiteral<io.quarkus.agroal.DataSource>
            implements io.quarkus.agroal.DataSource {

        private String name;

        public DataSourceLiteral(String name) {
            this.name = name;
        }

        @Override
        public String value() {
            return name;
        }

    }
}
