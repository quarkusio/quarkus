package io.quarkus.arc.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.arc.Unremovable;
import io.quarkus.deployment.annotations.BuildStep;

public class UnremovableAnnotationsProcessor {

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.targetWithAnnotation(DotName.createSimple(Unremovable.class.getName()));
    }
}
