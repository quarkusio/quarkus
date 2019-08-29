package io.quarkus.scheduler.runtime;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.quartz.utils.PoolingConnectionProvider;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

public class AgroalQuartzConnectionPoolingProvider implements PoolingConnectionProvider {
    final private DataSource dataSource;

    public AgroalQuartzConnectionPoolingProvider() {
        SchedulerBuildTimeConfig.SchedulerDatasourceConfig dataSourceConfig = SchedulerConfigHolder
                .getSchedulerBuildTimeConfig().stateStore.datasource;
        final InstanceHandle<DataSource> dataSourceInstanceHandle;

        if (dataSourceConfig.name.isPresent()) {
            dataSourceInstanceHandle = Arc.container().instance(dataSourceConfig.name.get());
        } else {
            dataSourceInstanceHandle = Arc.container().instance(DataSource.class);
        }

        if (dataSourceInstanceHandle.isAvailable()) {
            this.dataSource = dataSourceInstanceHandle.get();
        } else {
            final String dataSourceName = dataSourceConfig.name.orElse("_default_");
            throw new IllegalStateException(
                    "JDBC Store configured but the datasource \"" + dataSourceName + "\" is missing. "
                            + "You can configure your "
                            + "datasource by following the guide available at: https://quarkus.io/guides/datasource-guide");
        }
    }

    @SuppressWarnings("unused")
    public AgroalQuartzConnectionPoolingProvider(Properties properties) {
        this();
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
    }

    @Override
    public void initialize() {

    }
}
