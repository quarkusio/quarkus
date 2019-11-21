package io.quarkus.qute.resteasy.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.resteasy.TemplateResponseFilter;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;

public class QuteResteasyProcessor {

    @BuildStep
    ResteasyJaxrsProviderBuildItem registerProviders() {
        return new ResteasyJaxrsProviderBuildItem(TemplateResponseFilter.class.getName());
    }

    @BuildStep
    ReflectiveHierarchyIgnoreWarningBuildItem ignoreReflectiveWarning() {
        return new ReflectiveHierarchyIgnoreWarningBuildItem(DotName.createSimple(TemplateInstance.class.getName()));
    }

}
