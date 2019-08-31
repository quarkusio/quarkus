package io.quarkus.jackson.deployment;

import java.util.Collection;

import javax.inject.Inject;

import org.jboss.jandex.*;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;

public class JacksonProcessor {

    private static final DotName JSON_DESERIALIZE = DotName.createSimple(JsonDeserialize.class.getName());
    private static final DotName BUILDER_VOID = DotName.createSimple(Void.class.getName());

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass;

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @BuildStep
    void register() {
        addReflectiveClass(true, false,
                "com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector",
                "com.fasterxml.jackson.databind.ser.std.SqlDateSerializer");

        IndexView index = combinedIndexBuildItem.getIndex();

        // TODO: Here we only check for @JsonDeserialize to detect both Model and Builder
        //       classes to support @JsonPojoBuilder. The @JsonDeserialize annotiona can
        //       also be used for other scenarios than builder beside adding just the class
        //       no other scenarios are supported (like when the annotation is place on
        //       methods).

        Collection<AnnotationInstance> pojoBuilderInstances = index.getAnnotations(JSON_DESERIALIZE);
        for (AnnotationInstance pojoBuilderInstance : pojoBuilderInstances) {
            if (AnnotationTarget.Kind.CLASS.equals(pojoBuilderInstance.target().kind())) {
                addReflectiveHierarchyClass(pojoBuilderInstance.target().asClass().name());

                AnnotationValue annotationValue = pojoBuilderInstance.value("builder");
                if (null != annotationValue && AnnotationValue.Kind.CLASS.equals(annotationValue.kind())) {
                    DotName builderClassName = annotationValue.asClass().name();
                    if (!BUILDER_VOID.equals(builderClassName)) {
                        addReflectiveHierarchyClass(builderClassName);
                    }
                }
            }
        }
    }

    private void addReflectiveHierarchyClass(DotName className) {
        Type jandexType = Type.create(className, Type.Kind.CLASS);
        reflectiveHierarchyClass.produce(new ReflectiveHierarchyBuildItem(jandexType));
    }

    private void addReflectiveClass(boolean methods, boolean fields, String... className) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(methods, fields, className));
    }
}
