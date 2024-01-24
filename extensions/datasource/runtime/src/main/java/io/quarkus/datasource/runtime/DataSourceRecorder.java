package io.quarkus.datasource.runtime;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DataSourceRecorder {

    public RuntimeValue<DataSourceSupport> createDataSourceSupport(
            DataSourcesBuildTimeConfig buildTimeConfig,
            DataSourcesRuntimeConfig runtimeConfig) {
        Stream.Builder<String> configured = Stream.builder();
        Stream.Builder<String> excludedForHealthChecks = Stream.builder();
        for (Map.Entry<String, DataSourceBuildTimeConfig> dataSource : buildTimeConfig.dataSources().entrySet()) {
            // TODO this is wrong, as the default datasource could be configured without db-kind being set:
            //  it's inferred automatically for the default datasource when possible.
            //  See https://github.com/quarkusio/quarkus/issues/37779
            if (dataSource.getValue().dbKind().isPresent()) {
                configured.add(dataSource.getKey());
            }
            if (dataSource.getValue().healthExclude()) {
                excludedForHealthChecks.add(dataSource.getKey());
            }
        }
        Set<String> names = configured.build().collect(toUnmodifiableSet());
        Set<String> excludedNames = excludedForHealthChecks.build().collect(toUnmodifiableSet());

        Stream.Builder<String> inactive = Stream.builder();
        for (Map.Entry<String, DataSourceRuntimeConfig> entry : runtimeConfig.dataSources().entrySet()) {
            if (!entry.getValue().active()) {
                inactive.add(entry.getKey());
            }
        }
        Set<String> inactiveNames = inactive.build().collect(toUnmodifiableSet());

        return new RuntimeValue<>(new DataSourceSupport(names, excludedNames, inactiveNames));
    }
}
