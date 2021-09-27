package io.quarkus.datasource.runtime;

import java.util.Set;

public class DataSourcesExcludedFromHealthChecks {

    private final Set<String> excludedNames;

    public DataSourcesExcludedFromHealthChecks(Set<String> excludedNames) {
        this.excludedNames = excludedNames;
    }

    public Set<String> getExcludedNames() {
        return excludedNames;
    }
}
