package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import com.fasterxml.jackson.annotation.JsonView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.jackson.CustomSerialization;

public class ResteasyReactiveCommonJacksonProcessor {

    private static final DotName JSON_VIEW = DotName.createSimple(JsonView.class.getName());
    private static final DotName CUSTOM_SERIALIZATION = DotName.createSimple(CustomSerialization.class.getName());

    @BuildStep
    void handleJsonAnnotations(Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            CombinedIndexBuildItem index,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            BuildProducer<JacksonFeatureBuildItem> jacksonFeaturesProducer) {
        if (!resourceScanningResultBuildItem.isPresent()) {
            return;
        }
        Collection<ClassInfo> resourceClasses = resourceScanningResultBuildItem.get().getResult().getScannedResources()
                .values();
        Set<String> classesNeedingReflectionOnMethods = new HashSet<>();
        Set<JacksonFeatureBuildItem.Feature> jacksonFeatures = new HashSet<>();
        for (ClassInfo resourceClass : resourceClasses) {
            DotName resourceClassDotName = resourceClass.name();
            if (resourceClass.annotations().containsKey(JSON_VIEW)) {
                classesNeedingReflectionOnMethods.add(resourceClassDotName.toString());
                jacksonFeatures.add(JacksonFeatureBuildItem.Feature.JSON_VIEW);
            } else if (resourceClass.annotations().containsKey(CUSTOM_SERIALIZATION)) {
                classesNeedingReflectionOnMethods.add(resourceClassDotName.toString());
                jacksonFeatures.add(JacksonFeatureBuildItem.Feature.CUSTOM_SERIALIZATION);
                for (AnnotationInstance instance : resourceClass.annotations().get(CUSTOM_SERIALIZATION)) {
                    AnnotationValue annotationValue = instance.value();
                    if (annotationValue != null) {
                        Type biFunctionType = annotationValue.asClass();
                        ClassInfo biFunctionClassInfo = index.getIndex().getClassByName(biFunctionType.name());
                        if (biFunctionClassInfo == null) {
                            // be lenient
                        } else {
                            if (!biFunctionClassInfo.hasNoArgsConstructor()) {
                                throw new RuntimeException(
                                        "Class '" + biFunctionClassInfo.name() + "' must contain a no-args constructor");
                            }
                        }
                        reflectiveClassProducer.produce(
                                new ReflectiveClassBuildItem(true, false, false, biFunctionType.name().toString()));
                    }
                }
            }
        }
        if (!classesNeedingReflectionOnMethods.isEmpty()) {
            reflectiveClassProducer.produce(
                    new ReflectiveClassBuildItem(true, false, classesNeedingReflectionOnMethods.toArray(new String[0])));
        }
        if (!jacksonFeatures.isEmpty()) {
            for (JacksonFeatureBuildItem.Feature jacksonFeature : jacksonFeatures) {
                jacksonFeaturesProducer.produce(new JacksonFeatureBuildItem(jacksonFeature));
            }
        }
    }
}
