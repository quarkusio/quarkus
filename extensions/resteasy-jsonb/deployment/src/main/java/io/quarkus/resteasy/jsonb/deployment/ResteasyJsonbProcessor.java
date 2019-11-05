package io.quarkus.resteasy.jsonb.deployment;

import java.util.HashSet;
import java.util.Set;

import javax.json.bind.Jsonb;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.jsonb.runtime.QuarkusJsonbContextResolver;

public class ResteasyJsonbProcessor {

    private static final DotName JSONB = DotName.createSimple(Jsonb.class.getName());

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.RESTEASY_JSON_EXTENSION);
    }

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY_JSONB));
    }

    @BuildStep
    void register(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ResteasyJaxrsProviderBuildItem> jaxrsProvider,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<UnremovableBeanBuildItem> unremovable) {

        IndexView index = combinedIndexBuildItem.getIndex();

        jaxrsProvider.produce(new ResteasyJaxrsProviderBuildItem(QuarkusJsonbContextResolver.class.getName()));

        // this needs to be registered manually since the runtime module is not indexed by Jandex
        additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(QuarkusJsonbContextResolver.class));
        Set<String> userSuppliedProducers = getUserSuppliedJsonbProducerBeans(index);
        if (!userSuppliedProducers.isEmpty()) {
            unremovable.produce(
                    new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanClassNamesExclusion(userSuppliedProducers)));
        }
    }

    /*
     * We need to find all the user supplied producers and mark them as unremovable since there might be no injection points
     * for the Jsonb
     */
    private Set<String> getUserSuppliedJsonbProducerBeans(IndexView index) {
        Set<String> result = new HashSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(DotNames.PRODUCES)) {
            if (annotation.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }
            if (JSONB.equals(annotation.target().asMethod().returnType().name())) {
                result.add(annotation.target().asMethod().declaringClass().name().toString());
            }
        }
        return result;
    }
}
