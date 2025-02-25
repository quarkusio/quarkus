package io.quarkus.datasource.common.runtime;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.quarkus.runtime.configuration.ConfigurationException;

public final class DataSourceUtil {

    public static final String DEFAULT_DATASOURCE_NAME = "<default>";

    public static boolean isDefault(String dataSourceName) {
        return dataSourceName == null || DEFAULT_DATASOURCE_NAME.equals(dataSourceName);
    }

    public static boolean hasDefault(Collection<String> dataSourceNames) {
        return dataSourceNames.contains(DEFAULT_DATASOURCE_NAME);
    }

    public static String dataSourcePropertyKey(String datasourceName, String radical) {
        if (DataSourceUtil.isDefault(datasourceName)) {
            return "quarkus.datasource." + radical;
        } else {
            return "quarkus.datasource.\"" + datasourceName + "\"." + radical;
        }
    }

    public static List<String> dataSourcePropertyKeys(String datasourceName, String radical) {
        if (DataSourceUtil.isDefault(datasourceName)) {
            return List.of("quarkus.datasource." + radical);
        } else {
            // Two possible syntaxes: with or without quotes
            return List.of(
                    "quarkus.datasource.\"" + datasourceName + "\"." + radical,
                    "quarkus.datasource." + datasourceName + "." + radical);
        }
    }

    public static ConfigurationException dataSourceNotConfigured(String dataSourceName) {
        return new ConfigurationException(String.format(Locale.ROOT,
                "Datasource '%s' is not configured."
                        + " To solve this, configure datasource '%s'."
                        + " Refer to https://quarkus.io/guides/datasource for guidance.",
                dataSourceName, dataSourceName),
                Set.of(dataSourcePropertyKey(dataSourceName, "db-kind"),
                        dataSourcePropertyKey(dataSourceName, "username"),
                        dataSourcePropertyKey(dataSourceName, "password"),
                        dataSourcePropertyKey(dataSourceName, "jdbc.url")));
    }

    /**
     * @deprecated Simply call {@code io.quarkus.arc.ClientProxy#unwrap(Object)} on a datasource bean instance:
     *             it will throw a similar exception, with more details and an actionable message.
     */
    @Deprecated
    public static ConfigurationException dataSourceInactive(String dataSourceName) {
        return new ConfigurationException(dataSourceInactiveReasonDeactivated(dataSourceName),
                Set.of(dataSourcePropertyKey(dataSourceName, "db-kind"),
                        dataSourcePropertyKey(dataSourceName, "username"),
                        dataSourcePropertyKey(dataSourceName, "password"),
                        dataSourcePropertyKey(dataSourceName, "jdbc.url")));
    }

    public static String dataSourceInactiveReasonDeactivated(String dataSourceName) {
        return String.format(Locale.ROOT,
                "Datasource '%s' was deactivated through configuration properties."
                        + " To activate the datasource, set configuration property '%s' to 'true' and configure datasource '%s'."
                        + " Refer to https://quarkus.io/guides/datasource for guidance.",
                dataSourceName, dataSourcePropertyKey(dataSourceName, "active"), dataSourceName);
    }

    public static String dataSourceInactiveReasonUrlMissing(String dataSourceName, String urlPropertyRadical) {
        return String.format(Locale.ROOT,
                "Datasource '%s' was deactivated automatically because its URL is not set."
                        + " To activate the datasource, set configuration property '%s'."
                        + " Refer to https://quarkus.io/guides/datasource for guidance.",
                dataSourceName, dataSourcePropertyKey(dataSourceName, urlPropertyRadical));
    }

    private DataSourceUtil() {
    }

}
