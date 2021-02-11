package io.quarkus.elytron.security.jdbc;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Container for a default and optionals named {@linkplain io.quarkus.elytron.security.runtime.jdbc.PrincipalQueryConfig}
 */
@ConfigGroup
public class PrincipalQueriesConfig {

    /**
     * The default principal query
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public PrincipalQueryConfig defaultPrincipalQuery;

    /**
     * Additional principal queries
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, PrincipalQueryConfig> namedPrincipalQueries;

    @Override
    public String toString() {
        return "PrincipalQueriesConfig{" +
                "defaultPrincipalQuery=" + defaultPrincipalQuery +
                ", namedPrincipalQueries=" + namedPrincipalQueries +
                '}';
    }
}
