package io.quarkus.arc.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.deployment.builditem.TestClassBeanBuildItem;

public class TestsAsBeansProcessor {

    @BuildStep
    public void testAnnotations(List<TestAnnotationBuildItem> items, BuildProducer<BeanDefiningAnnotationBuildItem> producer) {
        for (TestAnnotationBuildItem item : items) {
            producer.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple(item.getAnnotationClassName())));
        }
    }

    @BuildStep
    public void testClassBeans(List<TestClassBeanBuildItem> items, BuildProducer<AdditionalBeanBuildItem> producer) {
        if (items.isEmpty()) {
            return;
        }

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        for (TestClassBeanBuildItem item : items) {
            builder.addBeanClass(item.getTestClassName());
        }
        producer.produce(builder.build());
    }

}
