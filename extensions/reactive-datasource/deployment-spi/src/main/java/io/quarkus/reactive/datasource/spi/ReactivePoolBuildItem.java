package io.quarkus.reactive.datasource.spi;

import java.util.function.Function;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.vertx.sqlclient.Pool;

/**
 * Build item produced by each reactive SQL client extension (PG, MySQL, MSSQL, etc.)
 * to signal pool creation. Consumed by the reactive-datasource extension to register
 * synthetic CDI beans ({@link Pool}, Mutiny Pool) and health checks.
 */
public final class ReactivePoolBuildItem extends MultiBuildItem {

    private final String dataSourceName;
    private final String dbType;
    private final Function<SyntheticCreationalContext<Pool>, Pool> pool;
    private final String healthCheckSql;

    public ReactivePoolBuildItem(String dataSourceName, String dbType,
            Function<SyntheticCreationalContext<Pool>, Pool> pool, String healthCheckSql) {
        this.dataSourceName = dataSourceName;
        this.pool = pool;
        this.dbType = dbType;
        this.healthCheckSql = healthCheckSql;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public Function<SyntheticCreationalContext<Pool>, Pool> getPool() {
        return pool;
    }

    public boolean isDefault() {
        return DataSourceUtil.isDefault(dataSourceName);
    }

    public String getDbType() {
        return dbType;
    }

    public String getHealthCheckSql() {
        return healthCheckSql;
    }
}
