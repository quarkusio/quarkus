package io.quarkus.agroal.deployment;

import java.util.Map;

import io.quarkus.agroal.runtime.DataSourceJdbcBuildTimeConfig;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;

final class AggregatedDataSourceBuildTimeConfigBuildItem extends MultiBuildItem {

    private final String name;

    private final DataSourceBuildTimeConfig dataSourceConfig;

    private final DataSourceJdbcBuildTimeConfig jdbcConfig;

    private final String dbKind;

    private final String resolvedDriverClass;

    private final Map<String, String> jdbcDriverProperties;

    AggregatedDataSourceBuildTimeConfigBuildItem(String name, DataSourceBuildTimeConfig dataSourceConfig,
            DataSourceJdbcBuildTimeConfig jdbcConfig,
            String dbKind,
            String resolvedDriverClass,
            Map<String, String> jdbcDriverProperties) {
        this.name = name;
        this.dataSourceConfig = dataSourceConfig;
        this.jdbcConfig = jdbcConfig;
        this.dbKind = dbKind;
        this.resolvedDriverClass = resolvedDriverClass;
        this.jdbcDriverProperties = jdbcDriverProperties;
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

    public Map<String, String> getJdbcDriverProperties() {
        return jdbcDriverProperties;
    }
}
