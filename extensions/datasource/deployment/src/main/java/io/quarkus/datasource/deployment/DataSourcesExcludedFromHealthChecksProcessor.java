package io.quarkus.datasource.deployment;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourceRecorder;
import io.quarkus.datasource.runtime.DataSourceSupport;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

public class DataSourcesExcludedFromHealthChecksProcessor {

    @BuildStep
    void produceBean(DataSourcesBuildTimeConfig buildTimeConfig,
            ServiceRegistrar serviceRegistrar,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        Set<String> healthCheckExcludedNames = Set.copyOf(buildTimeConfig.dataSources().entrySet().stream()
                .filter(dataSource -> dataSource.getValue().healthExclude())
                .map(Map.Entry<String, DataSourceBuildTimeConfig>::getKey)
                .collect(Collectors.toSet()));

        serviceRegistrar.forService(DataSourceSupport.class)
                .atPhase(Phase.CONFIG)
                .require(DataSourcesRuntimeConfig.class)
                .onStart((ctx, runtimeConfig) -> DataSourceRecorder.createDataSourceSupport(healthCheckExcludedNames,
                        runtimeConfig));

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(DataSourceSupport.class)
                .scope(Singleton.class)
                .unremovable()
                .serviceValue(DataSourceSupport.class)
                .setRuntimeInit()
                .done());
    }
}
