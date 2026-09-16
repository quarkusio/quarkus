package io.quarkus.reactive.datasource.deployment;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.reactive.datasource.runtime.DataSourceReactiveBuildTimeConfig;

/**
 * The first build item created after the decision was taken to define a datasource.
 * <p>
 * It holds build-time configuration and various datasource-related information that is resolved early.
 */
public final class ReactiveDataSourceDefinitionBuildItem extends MultiBuildItem {

    private final String name;

    private final DataSourceBuildTimeConfig dataSourceConfig;

    private final DataSourceReactiveBuildTimeConfig reactiveConfig;

    private final String dbKind;
    private final Optional<String> dbVersion;

    public ReactiveDataSourceDefinitionBuildItem(String name, DataSourceBuildTimeConfig dataSourceConfig,
            DataSourceReactiveBuildTimeConfig reactiveConfig,
            String dbKind,
            Optional<String> dbVersion) {
        this.name = name;
        this.dataSourceConfig = dataSourceConfig;
        this.reactiveConfig = reactiveConfig;
        this.dbKind = dbKind;
        this.dbVersion = dbVersion;
    }

    public String getName() {
        return name;
    }

    public DataSourceBuildTimeConfig getDataSourceConfig() {
        return dataSourceConfig;
    }

    public DataSourceReactiveBuildTimeConfig getReactiveConfig() {
        return reactiveConfig;
    }

    public boolean isDefault() {
        return DataSourceUtil.isDefault(name);
    }

    public String getDbKind() {
        return dbKind;
    }

    public Optional<String> getDbVersion() {
        return dbVersion;
    }
}
