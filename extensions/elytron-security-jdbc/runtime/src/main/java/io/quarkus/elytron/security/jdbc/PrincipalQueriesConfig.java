package io.quarkus.elytron.security.jdbc;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithParentName;

/**
 * Container for a default and optionals named {@linkplain io.quarkus.elytron.security.runtime.jdbc.PrincipalQueryConfig}
 */
@ConfigGroup
public interface PrincipalQueriesConfig {

    /**
     * The default principal query
     */
    @WithParentName
    PrincipalQueryConfig defaultPrincipalQuery();

    /**
     * Named queries.
     */
    @WithParentName
    @ConfigDocMapKey("query-name")
    @ConfigDocSection
    Map<String, PrincipalQueryConfig> namedPrincipalQueries();

    String toString();
}
