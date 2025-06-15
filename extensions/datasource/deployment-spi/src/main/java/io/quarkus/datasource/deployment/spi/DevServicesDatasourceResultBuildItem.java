package io.quarkus.datasource.deployment.spi;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;

public final class DevServicesDatasourceResultBuildItem extends SimpleBuildItem {

    final Map<String, DbResult> dataSources;

    public DevServicesDatasourceResultBuildItem(Map<String, DbResult> dataSources) {
        this.dataSources = Collections.unmodifiableMap(dataSources);
    }

    public DbResult getDefaultDatasource() {
        return dataSources.get(DataSourceUtil.DEFAULT_DATASOURCE_NAME);
    }

    public Map<String, DbResult> getNamedDatasources() {
        return dataSources.entrySet().stream().filter(e -> !DataSourceUtil.isDefault(e.getKey()))
                .collect(Collectors.toUnmodifiableMap(e -> e.getKey(), e -> e.getValue()));
    }

    public Map<String, DbResult> getDatasources() {
        return dataSources;
    }

    public static DbResult resolve(Optional<DevServicesDatasourceResultBuildItem> devDbResultBuildItem,
            String dataSourceName) {
        if (devDbResultBuildItem.isPresent()) {
            return devDbResultBuildItem.get().dataSources.get(dataSourceName);
        }
        return null;
    }

    public static class DbResult {
        final String dbType;
        final Map<String, String> configProperties;

        public DbResult(String dbType, Map<String, String> configProperties) {
            this.dbType = dbType;
            this.configProperties = Collections.unmodifiableMap(configProperties);
        }

        public String getDbType() {
            return dbType;
        }

        public Map<String, String> getConfigProperties() {
            return configProperties;
        }
    }
}
