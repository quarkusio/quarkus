package io.quarkus.oidc.token.propagation.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.EnableAllSecurityServicesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.oidc.deployment.OidcBuildStep.IsEnabled;
import io.quarkus.oidc.token.propagation.AccessToken;
import io.quarkus.oidc.token.propagation.AccessTokenRequestFilter;
import io.quarkus.oidc.token.propagation.runtime.OidcTokenPropagationConfig;
import io.quarkus.restclient.deployment.RestClientAnnotationProviderBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;

public class OidcTokenPropagationBuildStep {

    private static final DotName TOKEN_CREDENTIAL = DotName.createSimple(AccessToken.class.getName());

    OidcTokenPropagationConfig config;

    @BuildStep(onlyIf = IsEnabled.class)
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.OIDC_TOKEN_PROPAGATION);
    }

    @BuildStep(onlyIf = IsEnabled.class)
    EnableAllSecurityServicesBuildItem security() {
        return new EnableAllSecurityServicesBuildItem();
    }

    @BuildStep(onlyIf = IsEnabled.class)
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ResteasyJaxrsProviderBuildItem> jaxrsProviders,
            BuildProducer<RestClientAnnotationProviderBuildItem> restAnnotationProvider) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(AccessTokenRequestFilter.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, AccessTokenRequestFilter.class));
        if (config.registerFilter) {
            jaxrsProviders.produce(new ResteasyJaxrsProviderBuildItem(AccessTokenRequestFilter.class.getName()));
        } else {
            restAnnotationProvider.produce(new RestClientAnnotationProviderBuildItem(TOKEN_CREDENTIAL,
                    AccessTokenRequestFilter.class));
        }
    }
}
