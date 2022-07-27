package io.quarkus.oidc.client.filter.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.oidc.client.deployment.OidcClientBuildStep.IsEnabled;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.quarkus.oidc.client.filter.OidcClientRequestFilter;
import io.quarkus.oidc.client.filter.runtime.OidcClientFilterConfig;
import io.quarkus.restclient.deployment.RestClientAnnotationProviderBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;

@BuildSteps(onlyIf = IsEnabled.class)
public class OidcClientFilterBuildStep {

    private static final DotName OIDC_CLIENT_FILTER = DotName.createSimple(OidcClientFilter.class.getName());

    OidcClientFilterConfig config;

    @BuildStep
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ResteasyJaxrsProviderBuildItem> jaxrsProviders,
            BuildProducer<RestClientAnnotationProviderBuildItem> restAnnotationProvider) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(OidcClientRequestFilter.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, OidcClientRequestFilter.class));
        if (config.registerFilter) {
            jaxrsProviders.produce(new ResteasyJaxrsProviderBuildItem(OidcClientRequestFilter.class.getName()));
        } else {
            restAnnotationProvider.produce(new RestClientAnnotationProviderBuildItem(OIDC_CLIENT_FILTER,
                    OidcClientRequestFilter.class));
        }
    }
}
