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
 * <p>
 * The pool is either created by the {@link Pool} service registered under the data source name,
 * or, for extensions that still use a recorder, by the given creation function.
 */
public final class ReactivePoolBuildItem extends MultiBuildItem {

    private final String dataSourceName;
    private final String dbType;
    private final Function<SyntheticCreationalContext<Pool>, Pool> pool;
    private final String healthCheckSql;

    public ReactivePoolBuildItem(String dataSourceName, String dbType, String healthCheckSql) {
        this.dataSourceName = dataSourceName;
        this.pool = null;
        this.dbType = dbType;
        this.healthCheckSql = healthCheckSql;
    }

    /**
     * @deprecated register a {@link Pool} service named after the data source and use
     *             {@link #ReactivePoolBuildItem(String, String, String)} instead
     */
    @Deprecated(forRemoval = true)
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

    /**
     * @return the recorder-based creation function, or {@code null} when the pool is provided by the
     *         {@link Pool} service named after the data source
     */
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
