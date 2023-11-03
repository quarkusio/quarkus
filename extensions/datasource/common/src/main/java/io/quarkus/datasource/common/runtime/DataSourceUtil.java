package io.quarkus.datasource.common.runtime;

import java.util.Collection;
import java.util.List;

public final class DataSourceUtil {

    public static final String DEFAULT_DATASOURCE_NAME = "<default>";

    public static boolean isDefault(String dataSourceName) {
        return DEFAULT_DATASOURCE_NAME.equals(dataSourceName);
    }

    public static boolean hasDefault(Collection<String> dataSourceNames) {
        return dataSourceNames.contains(DEFAULT_DATASOURCE_NAME);
    }

    public static String dataSourcePropertyKey(String datasourceName, String radical) {
        if (datasourceName == null || DataSourceUtil.isDefault(datasourceName)) {
            return "quarkus.datasource." + radical;
        } else {
            return "quarkus.datasource.\"" + datasourceName + "\"." + radical;
        }
    }

    public static List<String> dataSourcePropertyKeys(String datasourceName, String radical) {
        if (datasourceName == null || DataSourceUtil.isDefault(datasourceName)) {
            return List.of("quarkus.datasource." + radical);
        } else {
            // Two possible syntaxes: with or without quotes
            return List.of(
                    "quarkus.datasource.\"" + datasourceName + "\"." + radical,
                    "quarkus.datasource." + datasourceName + "." + radical);
        }
    }

    private DataSourceUtil() {
    }

}
