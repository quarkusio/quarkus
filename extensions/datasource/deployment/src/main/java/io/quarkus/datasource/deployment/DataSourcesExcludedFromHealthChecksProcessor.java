package io.quarkus.datasource.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import javax.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesExcludedFromHealthChecks;
import io.quarkus.datasource.runtime.DataSourcesExcludedFromHealthChecksRecorder;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;

public class DataSourcesExcludedFromHealthChecksProcessor {

    @BuildStep
    @Record(STATIC_INIT)
    void produceBean(
            Capabilities capabilities,
            DataSourcesExcludedFromHealthChecksRecorder recorder,
            DataSourcesBuildTimeConfig config,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        if (capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            syntheticBeans.produce(SyntheticBeanBuildItem.configure(DataSourcesExcludedFromHealthChecks.class)
                    .scope(Singleton.class)
                    .unremovable()
                    .runtimeValue(recorder.configureDataSourcesExcludedFromHealthChecks(config))
                    .done());
        }
    }
}
