package io.quarkus.oidc.token.propagation.deployment;

import static io.quarkus.oidc.token.propagation.common.runtime.TokenPropagationConstants.JWT_PROPAGATE_TOKEN_CREDENTIAL;
import static io.quarkus.oidc.token.propagation.common.runtime.TokenPropagationConstants.OIDC_PROPAGATE_TOKEN_CREDENTIAL;

import java.util.List;
import java.util.function.BooleanSupplier;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.oidc.token.propagation.AccessTokenRequestFilter;
import io.quarkus.oidc.token.propagation.JsonWebToken;
import io.quarkus.oidc.token.propagation.JsonWebTokenRequestFilter;
import io.quarkus.oidc.token.propagation.common.deployment.AccessTokenInstanceBuildItem;
import io.quarkus.oidc.token.propagation.common.deployment.AccessTokenRequestFilterGenerator;
import io.quarkus.oidc.token.propagation.runtime.OidcTokenPropagationBuildTimeConfig;
import io.quarkus.oidc.token.propagation.runtime.OidcTokenPropagationConfig;
import io.quarkus.restclient.deployment.RestClientAnnotationProviderBuildItem;
import io.quarkus.restclient.deployment.RestClientPredicateProviderBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;

@BuildSteps(onlyIf = OidcTokenPropagationBuildStep.IsEnabled.class)
public class OidcTokenPropagationBuildStep {

    private static final DotName JWT_ACCESS_TOKEN_CREDENTIAL = DotName.createSimple(JsonWebToken.class.getName());

    OidcTokenPropagationConfig config;

    @BuildStep
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ResteasyJaxrsProviderBuildItem> jaxrsProviders,
            BuildProducer<RestClientPredicateProviderBuildItem> providerPredicateProducer,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeanProducer,
            List<AccessTokenInstanceBuildItem> accessTokenInstances,
            BuildProducer<RestClientAnnotationProviderBuildItem> restAnnotationProvider) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(AccessTokenRequestFilter.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(JsonWebTokenRequestFilter.class));
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(AccessTokenRequestFilter.class, JsonWebTokenRequestFilter.class)
                        .reason(getClass().getName())
                        .methods().fields().build());

        if (config.registerFilter()) {
            Class<?> filterClass = config.jsonWebToken() ? JsonWebTokenRequestFilter.class : AccessTokenRequestFilter.class;
            jaxrsProviders.produce(new ResteasyJaxrsProviderBuildItem(filterClass.getName()));
        } else {
            restAnnotationProvider.produce(new RestClientAnnotationProviderBuildItem(JWT_ACCESS_TOKEN_CREDENTIAL,
                    JsonWebTokenRequestFilter.class));
            if (!accessTokenInstances.isEmpty()) {
                var filterGenerator = new AccessTokenRequestFilterGenerator(unremovableBeanProducer, reflectiveClass,
                        generatedBeanProducer, AccessTokenRequestFilter.class);
                for (AccessTokenInstanceBuildItem instance : accessTokenInstances) {
                    String providerClass = filterGenerator.generateClass(instance);
                    providerPredicateProducer.produce(new RestClientPredicateProviderBuildItem(providerClass,
                            ci -> instance.targetClass().equals(ci.name().toString())));
                }
            }
        }
    }

    @BuildStep(onlyIf = IsEnabledDuringAuth.class)
    SystemPropertyBuildItem activateTokenCredentialPropagationViaDuplicatedContext(Capabilities capabilities) {
        if (capabilities.isPresent(Capability.OIDC)) {
            return new SystemPropertyBuildItem(OIDC_PROPAGATE_TOKEN_CREDENTIAL, "true");
        }

        if (capabilities.isPresent(Capability.JWT)) {
            return new SystemPropertyBuildItem(JWT_PROPAGATE_TOKEN_CREDENTIAL, "true");
        }

        throw new ConfigurationException(
                "Configuration property 'quarkus.resteasy-client-oidc-token-propagation.enabled-during-authentication' is set to "
                        +
                        "'true', however this configuration property is only supported when either 'quarkus-oidc' or " +
                        "'quarkus-smallrye-jwt' extensions are present.");
    }

    public static class IsEnabled implements BooleanSupplier {
        OidcTokenPropagationBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled();
        }
    }

    public static class IsEnabledDuringAuth implements BooleanSupplier {
        OidcTokenPropagationBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabledDuringAuthentication();
        }
    }
}
