package io.quarkus.arc.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;

public class TestsAsBeanDefiningAnnotationsProcessor {

    @BuildStep
    public void testsAsBeanDefiningAnnotations(LaunchModeBuildItem launchMode,
            BuildProducer<BeanDefiningAnnotationBuildItem> producer) {
        if (launchMode.getLaunchMode() != LaunchMode.TEST) {
            return;
        }

        producer.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple("io.quarkus.test.junit.QuarkusTest")));
        producer.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple("org.junit.runner.RunWith")));
    }

}
