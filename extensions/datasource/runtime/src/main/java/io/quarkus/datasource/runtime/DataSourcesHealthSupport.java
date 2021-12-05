package io.quarkus.datasource.runtime;

import java.util.Set;

/**
 * Helper class that holds the names of all configured data sources, along with the names of those
 * that are excluded from health checks. This is used by health check implementation classes.
 */
public class DataSourcesHealthSupport {

    private final Set<String> configuredNames;
    private final Set<String> excludedNames;

    public DataSourcesHealthSupport(Set<String> configuredNames, Set<String> excludedNames) {
        this.configuredNames = configuredNames;
        this.excludedNames = excludedNames;
    }

    public Set<String> getConfiguredNames() {
        return configuredNames;
    }

    public Set<String> getExcludedNames() {
        return excludedNames;
    }
}
