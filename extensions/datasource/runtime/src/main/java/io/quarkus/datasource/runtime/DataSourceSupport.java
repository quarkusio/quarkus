package io.quarkus.datasource.runtime;

import java.util.HashSet;
import java.util.Set;

/**
 * Helper class that holds the names of all configured data sources,
 * along with the names of those that are inactive or excluded from health checks.
 * <p>
 * This is used by any feature that needs runtime access to data sources,
 * e.g. Flyway/Liquibase or health check implementation classes.
 */
public class DataSourceSupport {

    private final Set<String> configuredNames;
    private final Set<String> inactiveNames;
    private final Set<String> inactiveOrHealthCheckExcludedNames;

    public DataSourceSupport(Set<String> configuredNames, Set<String> healthCheckExcludedNames,
            Set<String> inactiveNames) {
        this.configuredNames = configuredNames;
        this.inactiveOrHealthCheckExcludedNames = new HashSet<>();
        inactiveOrHealthCheckExcludedNames.addAll(inactiveNames);
        inactiveOrHealthCheckExcludedNames.addAll(healthCheckExcludedNames);
        this.inactiveNames = inactiveNames;
    }

    // TODO careful when using this, as it might (incorrectly) not include the default datasource.
    //   See TODO in code that calls the constructor of this class.
    //   See https://github.com/quarkusio/quarkus/issues/37779
    public Set<String> getConfiguredNames() {
        return configuredNames;
    }

    public Set<String> getInactiveNames() {
        return inactiveNames;
    }

    public Set<String> getInactiveOrHealthCheckExcludedNames() {
        return inactiveOrHealthCheckExcludedNames;
    }
}
