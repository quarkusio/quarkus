package io.quarkus.resteasy.reactive.qute.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.resteasy.reactive.qute.runtime.TemplateResponseFilter;
import io.quarkus.resteasy.reactive.spi.CustomContainerResponseFilterBuildItem;

public class QuarkusRestQuteProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.QUARKUS_REST_QUTE);
    }

    @BuildStep
    CustomContainerResponseFilterBuildItem registerProviders() {
        return new CustomContainerResponseFilterBuildItem(TemplateResponseFilter.class.getName());
    }

    @BuildStep
    ReflectiveHierarchyIgnoreWarningBuildItem ignoreReflectiveWarning() {
        return new ReflectiveHierarchyIgnoreWarningBuildItem(new ReflectiveHierarchyIgnoreWarningBuildItem.DotNameExclusion(
                DotName.createSimple(TemplateInstance.class.getName())));
    }

}
