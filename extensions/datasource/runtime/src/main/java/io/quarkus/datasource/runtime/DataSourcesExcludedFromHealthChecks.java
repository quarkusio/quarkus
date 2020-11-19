package io.quarkus.datasource.runtime;

import java.util.List;

public class DataSourcesExcludedFromHealthChecks {

    private List<String> excludedNames;

    public DataSourcesExcludedFromHealthChecks(List<String> names) {
        this.excludedNames = names;
    }

    public List<String> getExcludedNames() {
        return excludedNames;
    }
}
