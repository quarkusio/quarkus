package io.quarkus.oidc.token.propagation.reactive.deployment;

import static io.quarkus.oidc.token.propagation.common.runtime.TokenPropagationConstants.JWT_PROPAGATE_TOKEN_CREDENTIAL;
import static io.quarkus.oidc.token.propagation.common.runtime.TokenPropagationConstants.OIDC_PROPAGATE_TOKEN_CREDENTIAL;

import java.util.List;
import java.util.function.BooleanSupplier;

import jakarta.ws.rs.Priorities;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.oidc.token.propagation.common.deployment.AccessTokenInstanceBuildItem;
import io.quarkus.oidc.token.propagation.common.deployment.AccessTokenRequestFilterGenerator;
import io.quarkus.oidc.token.propagation.reactive.AccessTokenRequestReactiveFilter;
import io.quarkus.rest.client.reactive.deployment.DotNames;
import io.quarkus.rest.client.reactive.deployment.RegisterProviderAnnotationInstanceBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;

@BuildSteps(onlyIf = OidcTokenPropagationReactiveBuildStep.IsEnabled.class)
public class OidcTokenPropagationReactiveBuildStep {

    @BuildStep
    void oidcClientFilterSupport(List<AccessTokenInstanceBuildItem> accessTokenInstances,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            BuildProducer<RegisterProviderAnnotationInstanceBuildItem> providerProducer) {
        if (!accessTokenInstances.isEmpty()) {
            var filterGenerator = new AccessTokenRequestFilterGenerator(unremovableBeans, reflectiveClass, generatedBean,
                    AccessTokenRequestReactiveFilter.class);
            for (AccessTokenInstanceBuildItem instance : accessTokenInstances) {
                String providerClass = filterGenerator.generateClass(instance);
                providerProducer
                        .produce(new RegisterProviderAnnotationInstanceBuildItem(instance.targetClass(),
                                AnnotationInstance.create(DotNames.REGISTER_PROVIDER, instance.getAnnotationTarget(), List.of(
                                        AnnotationValue.createClassValue("value",
                                                Type.create(DotName.createSimple(providerClass),
                                                        org.jboss.jandex.Type.Kind.CLASS)),
                                        AnnotationValue.createIntegerValue("priority", Priorities.AUTHENTICATION)))));
            }
        }
    }

    @BuildStep
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItem) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(AccessTokenRequestReactiveFilter.class));
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(AccessTokenRequestReactiveFilter.class)
                .reason(getClass().getName())
                .methods().fields().build());
        additionalIndexedClassesBuildItem
                .produce(new AdditionalIndexedClassesBuildItem(AccessTokenRequestReactiveFilter.class.getName()));
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
                "Configuration property 'quarkus.rest-client-oidc-token-propagation.enabled-during-authentication' is set to " +
                        "'true', however this configuration property is only supported when either 'quarkus-oidc' or " +
                        "'quarkus-smallrye-jwt' extensions are present.");
    }

    public static class IsEnabled implements BooleanSupplier {
        OidcTokenPropagationReactiveBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled();
        }
    }

    public static class IsEnabledDuringAuth implements BooleanSupplier {
        OidcTokenPropagationReactiveBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabledDuringAuthentication();
        }
    }
}
