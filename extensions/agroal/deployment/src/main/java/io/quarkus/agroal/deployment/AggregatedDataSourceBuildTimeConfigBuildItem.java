package io.quarkus.agroal.deployment;

import io.quarkus.agroal.runtime.DataSourceJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.LegacyDataSourceJdbcBuildTimeConfig;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;

final class AggregatedDataSourceBuildTimeConfigBuildItem extends MultiBuildItem {

    private final String name;

    private final DataSourceBuildTimeConfig dataSourceConfig;

    private final DataSourceJdbcBuildTimeConfig jdbcConfig;

    private final LegacyDataSourceJdbcBuildTimeConfig legacyDataSourceJdbcConfig;

    private final String resolvedDbKind;

    private final String resolvedDriverClass;

    private boolean legacy;

    AggregatedDataSourceBuildTimeConfigBuildItem(String name, DataSourceBuildTimeConfig dataSourceConfig,
            DataSourceJdbcBuildTimeConfig jdbcConfig,
            LegacyDataSourceJdbcBuildTimeConfig legacyDataSourceJdbcConfig, String resolvedDbKind,
            String resolvedDriverClass, boolean legacy) {
        this.name = name;
        this.dataSourceConfig = dataSourceConfig;
        this.jdbcConfig = jdbcConfig;
        this.legacyDataSourceJdbcConfig = legacyDataSourceJdbcConfig;
        this.resolvedDbKind = resolvedDbKind;
        this.resolvedDriverClass = resolvedDriverClass;
        this.legacy = legacy;
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

    public LegacyDataSourceJdbcBuildTimeConfig getLegacyDataSourceJdbcConfig() {
        return legacyDataSourceJdbcConfig;
    }

    public boolean isDefault() {
        return DataSourceUtil.isDefault(name);
    }

    public String getResolvedDbKind() {
        return resolvedDbKind;
    }

    public String getResolvedDriverClass() {
        return resolvedDriverClass;
    }

    public boolean isLegacy() {
        return legacy;
    }
}
