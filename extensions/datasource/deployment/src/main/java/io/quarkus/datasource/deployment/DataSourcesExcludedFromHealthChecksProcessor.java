package io.quarkus.datasource.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.datasource.runtime.DataSourceRecorder;
import io.quarkus.datasource.runtime.DataSourceSupport;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;

public class DataSourcesExcludedFromHealthChecksProcessor {

    @BuildStep
    @Record(RUNTIME_INIT)
    void produceBean(
            Capabilities capabilities,
            DataSourceRecorder recorder,
            DataSourcesBuildTimeConfig buildTimeConfig, DataSourcesRuntimeConfig runtimeConfig,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(DataSourceSupport.class)
                .scope(Singleton.class)
                .unremovable()
                .runtimeValue(recorder.createDataSourceSupport(buildTimeConfig, runtimeConfig))
                .setRuntimeInit()
                .done());
    }
}
