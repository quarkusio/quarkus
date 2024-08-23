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

    private final Set<String> inactiveNames;
    private final Set<String> inactiveOrHealthCheckExcludedNames;

    public DataSourceSupport(Set<String> healthCheckExcludedNames,
            Set<String> inactiveNames) {
        this.inactiveOrHealthCheckExcludedNames = new HashSet<>();
        inactiveOrHealthCheckExcludedNames.addAll(inactiveNames);
        inactiveOrHealthCheckExcludedNames.addAll(healthCheckExcludedNames);
        this.inactiveNames = inactiveNames;
    }

    public Set<String> getInactiveNames() {
        return inactiveNames;
    }

    public Set<String> getInactiveOrHealthCheckExcludedNames() {
        return inactiveOrHealthCheckExcludedNames;
    }
}
