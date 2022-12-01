package io.quarkus.datasource.deployment.spi;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;

public final class DevServicesDatasourceResultBuildItem extends SimpleBuildItem {

    final DbResult defaultDatasource;
    final Map<String, DbResult> namedDatasources;

    public DevServicesDatasourceResultBuildItem(DbResult defaultDatasource, Map<String, DbResult> namedDatasources) {
        this.defaultDatasource = defaultDatasource;
        this.namedDatasources = Collections.unmodifiableMap(namedDatasources);
    }

    public DbResult getDefaultDatasource() {
        return defaultDatasource;
    }

    public Map<String, DbResult> getNamedDatasources() {
        return namedDatasources;
    }

    public static DbResult resolve(Optional<DevServicesDatasourceResultBuildItem> devDbResultBuildItem, String dataSourceName) {
        if (devDbResultBuildItem.isPresent()) {
            if (dataSourceName.equals(DataSourceUtil.DEFAULT_DATASOURCE_NAME)) {
                return devDbResultBuildItem.get().defaultDatasource;
            }
            return devDbResultBuildItem.get().namedDatasources.get(dataSourceName);
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
