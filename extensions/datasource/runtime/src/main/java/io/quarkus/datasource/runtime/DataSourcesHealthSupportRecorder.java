package io.quarkus.datasource.runtime;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DataSourcesHealthSupportRecorder {

    public RuntimeValue<DataSourcesHealthSupport> configureDataSourcesHealthSupport(
            DataSourcesBuildTimeConfig config) {
        Stream.Builder<String> configured = Stream.builder();
        Stream.Builder<String> excluded = Stream.builder();
        if (config.defaultDataSource.dbKind.isPresent()) {
            configured.add(DataSourceUtil.DEFAULT_DATASOURCE_NAME);
        }
        if (config.defaultDataSource.healthExclude) {
            excluded.add(DataSourceUtil.DEFAULT_DATASOURCE_NAME);
        }
        for (Map.Entry<String, DataSourceBuildTimeConfig> dataSource : config.namedDataSources.entrySet()) {
            configured.add(dataSource.getKey());
            if (dataSource.getValue().healthExclude) {
                excluded.add(dataSource.getKey());
            }
        }
        Set<String> names = configured.build().collect(toUnmodifiableSet());
        Set<String> excludedNames = excluded.build().collect(toUnmodifiableSet());
        return new RuntimeValue<>(new DataSourcesHealthSupport(names, excludedNames));
    }
}
