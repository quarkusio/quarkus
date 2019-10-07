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
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.jackson.runtime.QuarkusObjectMapperContextResolver;

public class ResteasyJacksonProcessor {

    private static final DotName OBJECT_MAPPER = DotName.createSimple(ObjectMapper.class.getName());

    @BuildStep(providesCapabilities = { Capabilities.RESTEASY_JSON_EXTENSION })
    void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY_JACKSON));
    }

    @BuildStep
    void register(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ResteasyJaxrsProviderBuildItem> jaxrsProvider,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<UnremovableBeanBuildItem> unremovable) {

        IndexView index = combinedIndexBuildItem.getIndex();

        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(QuarkusObjectMapperContextResolver.class.getName()));

        // this needs to be registered manually since the runtime module is not indexed by Jandex
        additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(QuarkusObjectMapperContextResolver.class));
        Set<String> userSuppliedProducers = getUserSuppliedJacksonProducerBeans(index);
        if (!userSuppliedProducers.isEmpty()) {
            unremovable.produce(
                    new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(userSuppliedProducers)));
        }
    }

    /*
     * We need to find all the user supplied producers and mark them as unremovable since there are no actual injection points
     * for the ObjectMapper
     */
    private Set<String> getUserSuppliedJacksonProducerBeans(IndexView index) {
        Set<String> result = new HashSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(DotNames.PRODUCES)) {
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
