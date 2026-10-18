package org.acme.deployment;

import java.util.Set;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import org.acme.AcmeRecorder;
import org.acme.RecordedWords;
import org.acme.WordProvider;

class AcmeProcessor {

    private static final String FEATURE = "acme";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(RecordedWords.class).build());
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void recordWords(AcmeRecorder recorder, BeanContainerBuildItem beanContainer) {
        recorder.recordWords(beanContainer.getValue(), WordProvider.loadAndSortWords(), Thread.currentThread().getContextClassLoader().getName());
    }
}
