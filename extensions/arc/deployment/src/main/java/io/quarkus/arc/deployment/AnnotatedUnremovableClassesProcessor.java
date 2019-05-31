package io.quarkus.arc.deployment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import io.quarkus.arc.Unremovable;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

public class AnnotatedUnremovableClassesProcessor {

    private static final DotName UNREMOVABLE = DotName.createSimple(Unremovable.class.getName());

    @BuildStep
    public void findAnnotatedClasses(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BuildProducer<UnremovableBeanBuildItem> producer) {
        final Set<AnnotationTarget> unremovableBeanTargets = new HashSet<>();
        final Collection<AnnotationInstance> unremovableAnnotations = beanArchiveIndexBuildItem.getIndex()
                .getAnnotations(UNREMOVABLE);
        for (AnnotationInstance annotationInstance : unremovableAnnotations) {
            unremovableBeanTargets.add(annotationInstance.target());
        }
        if (!unremovableBeanTargets.isEmpty()) {
            producer.produce(
                    new UnremovableBeanBuildItem(
                            b -> {
                                if (b.getTarget().isPresent()) {
                                    return unremovableBeanTargets.contains(b.getTarget().get());
                                }
                                return false;
                            }));
        }
    }
}
