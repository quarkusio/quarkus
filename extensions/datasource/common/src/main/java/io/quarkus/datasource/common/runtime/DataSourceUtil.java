package io.quarkus.datasource.common.runtime;

import java.util.Collection;

public final class DataSourceUtil {

    public static final String DEFAULT_DATASOURCE_NAME = "<default>";

    public static boolean isDefault(String dataSourceName) {
        return DEFAULT_DATASOURCE_NAME.equals(dataSourceName);
    }

    public static boolean hasDefault(Collection<String> dataSourceNames) {
        return dataSourceNames.contains(DEFAULT_DATASOURCE_NAME);
    }

    private DataSourceUtil() {
    }

}
