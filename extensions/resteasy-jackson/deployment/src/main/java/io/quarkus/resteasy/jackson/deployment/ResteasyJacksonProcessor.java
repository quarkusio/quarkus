package io.quarkus.resteasy.jackson.deployment;

import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.jackson.runtime.ObjectMapperContextResolver;
import io.quarkus.resteasy.jackson.runtime.ObjectMapperProducer;

public class ResteasyJacksonProcessor {

    private static final DotName OBJECT_MAPPER = DotName.createSimple(ObjectMapper.class.getName());

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY_JACKSON));
    }

    @BuildStep
    void register(CombinedIndexBuildItem combinedIndexBuildItem, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ResteasyJaxrsProviderBuildItem> jaxrsProvider,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<UnremovableBeanBuildItem> unremovable) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector",
                "com.fasterxml.jackson.databind.ser.std.SqlDateSerializer"));

        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(ObjectMapperContextResolver.class.getName()));

        additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(ObjectMapperProducer.class));
        Set<String> userSuppliedProducers = getUserSuppliedJacksonProducerBeans(combinedIndexBuildItem.getIndex());
        if (!userSuppliedProducers.isEmpty()) {
            unremovable.produce(
                    new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(userSuppliedProducers)));
        }
    }

    // we need to find all the user supplied producers and mark them as unremovable since there are no actual injection points
    // for the ObjectMapper
    private Set<String> getUserSuppliedJacksonProducerBeans(IndexView index) {
        Set<String> result = new HashSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(DotName.createSimple("javax.enterprise.inject.Produces"))) {
            if (annotation.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            if (OBJECT_MAPPER.equals(annotation.target().asMethod().returnType().name())) {
                result.add(annotation.target().asMethod().declaringClass().name().toString());
            }
        }
        return result;
    }
}
