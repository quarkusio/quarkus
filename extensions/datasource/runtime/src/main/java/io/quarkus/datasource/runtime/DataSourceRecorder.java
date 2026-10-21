package io.quarkus.datasource.runtime;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class DataSourceRecorder {

    public static DataSourceSupport createDataSourceSupport(Set<String> healthCheckExcludedNames,
            DataSourcesRuntimeConfig runtimeConfig) {
        Stream.Builder<String> inactive = Stream.builder();
        for (Map.Entry<String, DataSourceRuntimeConfig> entry : runtimeConfig.dataSources().entrySet()) {
            Optional<Boolean> active = entry.getValue().active();
            if (active.isPresent() && !active.get()) {
                inactive.add(entry.getKey());
            }
        }
        Set<String> inactiveNames = inactive.build().collect(toUnmodifiableSet());

        return new DataSourceSupport(healthCheckExcludedNames, inactiveNames);
    }
}
