package io.quarkus.agroal.deployment;

import io.quarkus.agroal.runtime.DataSourceJdbcBuildTimeConfig;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;

/**
 * An aggregated build item holding the build time configuration for a given datasource,
 * including both the build and the runtime datasource config.
 */
public final class AggregatedDataSourceBuildTimeConfigBuildItem extends MultiBuildItem {

    private final String name;

    private final DataSourceBuildTimeConfig dataSourceConfig;

    private final DataSourceJdbcBuildTimeConfig jdbcConfig;

    private final String dbKind;

    private final String resolvedDriverClass;

    AggregatedDataSourceBuildTimeConfigBuildItem(String name, DataSourceBuildTimeConfig dataSourceConfig,
            DataSourceJdbcBuildTimeConfig jdbcConfig,
            String dbKind,
            String resolvedDriverClass) {
        this.name = name;
        this.dataSourceConfig = dataSourceConfig;
        this.jdbcConfig = jdbcConfig;
        this.dbKind = dbKind;
        this.resolvedDriverClass = resolvedDriverClass;
    }

    public String getName() {
        return name;
    }

    public DataSourceBuildTimeConfig getDataSourceConfig() {
        return dataSourceConfig;
    }

    public DataSourceJdbcBuildTimeConfig getJdbcConfig() {
        return jdbcConfig;
    }

    public boolean isDefault() {
        return DataSourceUtil.isDefault(name);
    }

    public String getDbKind() {
        return dbKind;
    }

    public String getResolvedDriverClass() {
        return resolvedDriverClass;
    }
}
