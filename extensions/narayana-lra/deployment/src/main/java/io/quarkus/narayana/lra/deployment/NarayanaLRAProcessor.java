package io.quarkus.narayana.lra.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.narayana.lra.runtime.LRAConfiguration;
import io.quarkus.narayana.lra.runtime.NarayanaLRAProducers;
import io.quarkus.narayana.lra.runtime.NarayanaLRARecorder;

class NarayanaLRAProcessor {
    @BuildStep
    @Record(RUNTIME_INIT)
    public void build(NarayanaLRARecorder recorder,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit,
            LRAConfiguration configuration) {

        recorder.setConfig(configuration);
    }

    @BuildStep
    void registerFeature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.NARAYANA_LRA));
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(NarayanaLRAProducers.class));
    }
}
