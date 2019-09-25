package io.quarkus.jackson.deployment;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.jackson.ObjectMapperProducer;

public class JacksonProcessor {

    private static final DotName JSON_DESERIALIZE = DotName.createSimple(JsonDeserialize.class.getName());
    private static final DotName BUILDER_VOID = DotName.createSimple(Void.class.getName());

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass;

    @Inject
    BuildProducer<AdditionalBeanBuildItem> additionalBeans;

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @Inject
    List<IgnoreJsonDeserializeClassBuildItem> ignoreJsonDeserializeClassBuildItems;

    @BuildStep
    void register() {
        addReflectiveClass(true, false,
                "com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector",
                "com.fasterxml.jackson.databind.ser.std.SqlDateSerializer");

        IndexView index = combinedIndexBuildItem.getIndex();

        // TODO: @JsonDeserialize is only supported as a class annotation - we should support the others as well

        Set<DotName> ignoredDotNames = new HashSet<>();
        for (IgnoreJsonDeserializeClassBuildItem ignoreJsonDeserializeClassBuildItem : ignoreJsonDeserializeClassBuildItems) {
            ignoredDotNames.add(ignoreJsonDeserializeClassBuildItem.getDotName());
        }

        Collection<AnnotationInstance> pojoBuilderInstances = index.getAnnotations(JSON_DESERIALIZE);
        for (AnnotationInstance pojoBuilderInstance : pojoBuilderInstances) {
            if (AnnotationTarget.Kind.CLASS.equals(pojoBuilderInstance.target().kind())) {
                DotName dotName = pojoBuilderInstance.target().asClass().name();
                if (!ignoredDotNames.contains(dotName)) {
                    addReflectiveHierarchyClass(dotName);
                }

                AnnotationValue annotationValue = pojoBuilderInstance.value("builder");
                if (null != annotationValue && AnnotationValue.Kind.CLASS.equals(annotationValue.kind())) {
                    DotName builderClassName = annotationValue.asClass().name();
                    if (!BUILDER_VOID.equals(builderClassName)) {
                        addReflectiveHierarchyClass(builderClassName);
                    }
                }
            }
        }

        // this needs to be registered manually since the runtime module is not indexed by Jandex
        additionalBeans.produce(new AdditionalBeanBuildItem(ObjectMapperProducer.class));
    }

    private void addReflectiveHierarchyClass(DotName className) {
        Type jandexType = Type.create(className, Type.Kind.CLASS);
        reflectiveHierarchyClass.produce(new ReflectiveHierarchyBuildItem(jandexType));
    }

    private void addReflectiveClass(boolean methods, boolean fields, String... className) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(methods, fields, className));
    }
}
