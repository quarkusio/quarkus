package io.quarkus.datasource.runtime;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DataSourcesExcludedFromHealthChecksRecorder {

    public RuntimeValue<DataSourcesExcludedFromHealthChecks> configureDataSourcesExcludedFromHealthChecks(
            DataSourcesBuildTimeConfig config) {
        Stream.Builder<String> builder = Stream.builder();
        if (config.defaultDataSource.healthExclude) {
            builder.add(DataSourceUtil.DEFAULT_DATASOURCE_NAME);
        }
        for (Map.Entry<String, DataSourceBuildTimeConfig> dataSource : config.namedDataSources.entrySet()) {
            if (dataSource.getValue().healthExclude) {
                builder.add(dataSource.getKey());
            }
        }
        Set<String> excludedNames = builder.build().collect(toUnmodifiableSet());
        return new RuntimeValue<>(new DataSourcesExcludedFromHealthChecks(excludedNames));
    }
}
