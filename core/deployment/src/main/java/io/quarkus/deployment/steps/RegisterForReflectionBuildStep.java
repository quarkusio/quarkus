package io.quarkus.deployment.steps;

import javax.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class RegisterForReflectionBuildStep {

    @Inject
    BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @BuildStep
    public void build() throws Exception {
        for (AnnotationInstance i : combinedIndexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(RegisterForReflection.class.getName()))) {
            ClassInfo target = i.target().asClass();
            boolean methods = i.value("methods") == null || i.value("methods").asBoolean();
            boolean fields = i.value("fields") == null || i.value("fields").asBoolean();
            AnnotationValue targetsValue = i.value("targets");
            if (targetsValue == null) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(methods, fields, target.name().toString()));
            } else {
                Type[] targets = targetsValue.asClassArray();
                for (Type type : targets) {
                    reflectiveClass.produce(new ReflectiveClassBuildItem(methods, fields, type.name().toString()));
                }
            }
        }
    }

}
