package io.quarkus.datasource.runtime;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DataSourcesHealthSupportRecorder {

    public RuntimeValue<DataSourcesHealthSupport> configureDataSourcesHealthSupport(
            DataSourcesBuildTimeConfig config) {
        Stream.Builder<String> configured = Stream.builder();
        Stream.Builder<String> excluded = Stream.builder();
        for (Map.Entry<String, DataSourceBuildTimeConfig> dataSource : config.dataSources().entrySet()) {
            if (dataSource.getValue().dbKind().isPresent()) {
                configured.add(dataSource.getKey());
            }
            if (dataSource.getValue().healthExclude()) {
                excluded.add(dataSource.getKey());
            }
        }
        Set<String> names = configured.build().collect(toUnmodifiableSet());
        Set<String> excludedNames = excluded.build().collect(toUnmodifiableSet());
        return new RuntimeValue<>(new DataSourcesHealthSupport(names, excludedNames));
    }
}
