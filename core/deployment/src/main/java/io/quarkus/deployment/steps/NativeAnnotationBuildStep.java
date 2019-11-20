package io.quarkus.deployment.steps;

import javax.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.runtime.annotations.RuntimeInitialized;
import io.quarkus.runtime.annotations.RuntimeReinitialized;

public class NativeAnnotationBuildStep {

    @Inject
    BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit;

    @Inject
    BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReInit;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @Inject
    BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy;

    @BuildStep
    public void build() throws Exception {
        //reflection
        for (AnnotationInstance i : combinedIndexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(RegisterForReflection.class.getName()))) {
            ClassInfo target = i.target().asClass();
            boolean methods = i.value("methods") == null || i.value("methods").asBoolean();
            boolean fields = i.value("fields") == null || i.value("fields").asBoolean();
            boolean dependencies = i.value("registerDependencies") == null || i.value("registerDependencies").asBoolean();
            AnnotationValue targetsValue = i.value("targets");
            if (targetsValue == null) {
                registerForReflection(methods, fields, target.name(), dependencies);
            } else {
                Type[] targets = targetsValue.asClassArray();
                for (Type type : targets) {
                    registerForReflection(methods, fields, type.name(), dependencies);
                }
            }
        }

        //runtime re-init
        for (AnnotationInstance i : combinedIndexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(RuntimeReinitialized.class.getName()))) {
            ClassInfo target = i.target().asClass();
            AnnotationValue targetsValue = i.value("targets");
            if (targetsValue == null) {
                runtimeReInit.produce(new RuntimeReinitializedClassBuildItem(target.name().toString()));
            } else {
                Type[] targets = targetsValue.asClassArray();
                for (Type type : targets) {
                    runtimeReInit.produce(new RuntimeReinitializedClassBuildItem(type.name().toString()));
                }
            }
        }

        //runtime init
        for (AnnotationInstance i : combinedIndexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(RuntimeInitialized.class.getName()))) {
            ClassInfo target = i.target().asClass();
            AnnotationValue targetsValue = i.value("targets");
            if (targetsValue == null) {
                runtimeInit.produce(new RuntimeInitializedClassBuildItem(target.name().toString()));
            } else {
                Type[] targets = targetsValue.asClassArray();
                for (Type type : targets) {
                    runtimeInit.produce(new RuntimeInitializedClassBuildItem(type.name().toString()));
                }
            }
        }
    }

    private void registerForReflection(boolean methods, boolean fields, DotName name, boolean dependencies) {
        if (dependencies) {
            reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(ClassType.create(name, Type.Kind.CLASS)));
        } else {
            reflectiveClass.produce(new ReflectiveClassBuildItem(methods, fields, name.toString()));
        }
    }

}
