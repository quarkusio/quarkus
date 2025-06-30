package io.quarkus.datasource.runtime;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DataSourceRecorder {
    private final DataSourcesBuildTimeConfig buildTimeConfig;
    private final RuntimeValue<DataSourcesRuntimeConfig> runtimeConfig;

    public DataSourceRecorder(
            final DataSourcesBuildTimeConfig buildTimeConfig,
            final RuntimeValue<DataSourcesRuntimeConfig> runtimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
        this.runtimeConfig = runtimeConfig;
    }

    public RuntimeValue<DataSourceSupport> createDataSourceSupport() {
        Stream.Builder<String> excludedForHealthChecks = Stream.builder();
        for (Map.Entry<String, DataSourceBuildTimeConfig> dataSource : buildTimeConfig.dataSources().entrySet()) {
            if (dataSource.getValue().healthExclude()) {
                excludedForHealthChecks.add(dataSource.getKey());
            }
        }
        Set<String> excludedNames = excludedForHealthChecks.build().collect(toUnmodifiableSet());

        Stream.Builder<String> inactive = Stream.builder();
        for (Map.Entry<String, DataSourceRuntimeConfig> entry : runtimeConfig.getValue().dataSources().entrySet()) {
            Optional<Boolean> active = entry.getValue().active();
            if (active.isPresent() && !active.get()) {
                inactive.add(entry.getKey());
            }
        }
        Set<String> inactiveNames = inactive.build().collect(toUnmodifiableSet());

        return new RuntimeValue<>(new DataSourceSupport(excludedNames, inactiveNames));
    }
}
