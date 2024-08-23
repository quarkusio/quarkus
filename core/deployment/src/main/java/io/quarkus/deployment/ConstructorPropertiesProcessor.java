package io.quarkus.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * Registers all classes for reflection,
 * that contain a constructor annotated with <code>@java.beans.ConstructorProperties</code>.
 */
public class ConstructorPropertiesProcessor {

    private static final DotName CONSTRUCTOR_PROPERTIES = DotName.createSimple("java.beans.ConstructorProperties");

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem indexBuildItem) {
        IndexView index = indexBuildItem.getIndex();
        for (AnnotationInstance annotationInstance : index.getAnnotations(CONSTRUCTOR_PROPERTIES)) {
            registerInstance(reflectiveClass, annotationInstance);
        }
    }

    private void registerInstance(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, AnnotationInstance instance) {
        AnnotationTarget annotationTarget = instance.target();
        if (annotationTarget instanceof MethodInfo) {
            MethodInfo methodInfo = (MethodInfo) annotationTarget;
            String classname = methodInfo.declaringClass().toString();
            reflectiveClass.produce(asReflectiveClassBuildItem(classname));
        }
    }

    private ReflectiveClassBuildItem asReflectiveClassBuildItem(String annotatedClass) {
        return ReflectiveClassBuildItem.builder(annotatedClass).methods().build();
    }
}
