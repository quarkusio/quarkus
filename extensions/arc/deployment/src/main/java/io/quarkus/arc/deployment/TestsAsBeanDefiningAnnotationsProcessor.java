package io.quarkus.arc.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;

public class TestsAsBeanDefiningAnnotationsProcessor {

    @BuildStep
    public void produce(List<TestAnnotationBuildItem> items, BuildProducer<BeanDefiningAnnotationBuildItem> producer) {
        for (TestAnnotationBuildItem item : items) {
            producer.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple(item.getAnnotationClassName())));
        }
    }

}
