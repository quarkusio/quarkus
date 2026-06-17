package io.quarkus.spring.tx.deployment;

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;

public class SpringTransactionalProcessor {

    static final DotName SPRING_TRANSACTIONAL = DotName
            .createSimple("org.springframework.transaction.annotation.Transactional");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SPRING_TX);
    }

    @BuildStep
    AnnotationsTransformerBuildItem transform() {
        return new AnnotationsTransformerBuildItem(new SpringTransactionalAnnotationsTransformer());
    }

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    void validateTransactionalAnnotations(CombinedIndexBuildItem combinedIndex) {
        Collection<AnnotationInstance> instances = combinedIndex.getIndex()
                .getAnnotations(SPRING_TRANSACTIONAL);
        for (AnnotationInstance instance : instances) {
            AnnotationValue propagationValue = instance.value("propagation");
            if (propagationValue != null && "NESTED".equals(propagationValue.asEnum())) {
                throw new IllegalArgumentException(
                        "Spring @Transactional with propagation NESTED is not supported by Quarkus. "
                                + "Offending location: "
                                + SpringTransactionalUtil.describeTarget(instance.target()));
            }
        }
    }
}
