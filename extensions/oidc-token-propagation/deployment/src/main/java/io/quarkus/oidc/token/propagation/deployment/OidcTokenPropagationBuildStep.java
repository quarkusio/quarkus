package io.quarkus.oidc.token.propagation.deployment;

import java.util.function.BooleanSupplier;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.oidc.token.propagation.AccessToken;
import io.quarkus.oidc.token.propagation.AccessTokenRequestFilter;
import io.quarkus.oidc.token.propagation.JsonWebToken;
import io.quarkus.oidc.token.propagation.JsonWebTokenRequestFilter;
import io.quarkus.oidc.token.propagation.runtime.OidcTokenPropagationBuildTimeConfig;
import io.quarkus.oidc.token.propagation.runtime.OidcTokenPropagationConfig;
import io.quarkus.restclient.deployment.RestClientAnnotationProviderBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;

@BuildSteps(onlyIf = OidcTokenPropagationBuildStep.IsEnabled.class)
public class OidcTokenPropagationBuildStep {

    private static final DotName ACCESS_TOKEN_CREDENTIAL = DotName.createSimple(AccessToken.class.getName());
    private static final DotName JWT_ACCESS_TOKEN_CREDENTIAL = DotName.createSimple(JsonWebToken.class.getName());

    OidcTokenPropagationConfig config;

    @BuildStep
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ResteasyJaxrsProviderBuildItem> jaxrsProviders,
            BuildProducer<RestClientAnnotationProviderBuildItem> restAnnotationProvider) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(AccessTokenRequestFilter.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(JsonWebTokenRequestFilter.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, AccessTokenRequestFilter.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, JsonWebTokenRequestFilter.class));

        if (config.registerFilter) {
            Class<?> filterClass = config.jsonWebToken ? JsonWebTokenRequestFilter.class : AccessTokenRequestFilter.class;
            jaxrsProviders.produce(new ResteasyJaxrsProviderBuildItem(filterClass.getName()));
        } else {
            restAnnotationProvider.produce(new RestClientAnnotationProviderBuildItem(ACCESS_TOKEN_CREDENTIAL,
                    AccessTokenRequestFilter.class));
            restAnnotationProvider.produce(new RestClientAnnotationProviderBuildItem(JWT_ACCESS_TOKEN_CREDENTIAL,
                    JsonWebTokenRequestFilter.class));
        }
    }

    public static class IsEnabled implements BooleanSupplier {
        OidcTokenPropagationBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
