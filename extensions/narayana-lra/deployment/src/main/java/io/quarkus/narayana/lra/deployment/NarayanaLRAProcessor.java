package io.quarkus.narayana.lra.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import javax.inject.Inject;

import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import org.jboss.jandex.DotName;

import io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipantRegistry;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.narayana.lra.runtime.LRAConfiguration;
import io.quarkus.narayana.lra.runtime.NarayanaLRAProducers;
import io.quarkus.narayana.lra.runtime.NarayanaLRARecorder;

class NarayanaLRAProcessor {
    @Inject
    BuildProducer<AdditionalBeanBuildItem> additionalBeans;

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.LRA);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void build(
            NarayanaLRARecorder recorder, BuildProducer<FeatureBuildItem> feature, LRAConfiguration configuration) {
        feature.produce(new FeatureBuildItem(Feature.NARAYANA_LRA));

        additionalBeans.produce(new AdditionalBeanBuildItem(NarayanaLRAProducers.class));

        recorder.setConfig(configuration);
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem additionalBeanDefiningAnnotation() {
        return new BeanDefiningAnnotationBuildItem(
                DotName.createSimple(LRAParticipantRegistry.class.getName()));
    }
}
