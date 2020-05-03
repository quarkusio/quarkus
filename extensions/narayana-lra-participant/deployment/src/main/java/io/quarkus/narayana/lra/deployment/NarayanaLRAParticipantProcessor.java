package io.quarkus.narayana.lra.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import javax.inject.Inject;

import org.jboss.jandex.DotName;

import io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipantRegistry;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.narayana.lra.runtime.LRAParticipantConfiguration;
import io.quarkus.narayana.lra.runtime.NarayanaLRAParticipantProducers;
import io.quarkus.narayana.lra.runtime.NarayanaLRAParticipantRecorder;

class NarayanaLRAParticipantProcessor {
    @Inject
    BuildProducer<AdditionalBeanBuildItem> additionalBeans;

    @BuildStep(providesCapabilities = Capabilities.LRA_PARTICIPANT)
    @Record(RUNTIME_INIT)
    public void build(
            NarayanaLRAParticipantRecorder recorder, BuildProducer<FeatureBuildItem> feature,
            LRAParticipantConfiguration configuration) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.NARAYANA_LRA_PARTICIPANT));

        additionalBeans.produce(new AdditionalBeanBuildItem(NarayanaLRAParticipantProducers.class));

        recorder.setConfig(configuration);
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem additionalBeanDefiningAnnotation() {
        return new BeanDefiningAnnotationBuildItem(
                DotName.createSimple(LRAParticipantRegistry.class.getName()));
    }
}
