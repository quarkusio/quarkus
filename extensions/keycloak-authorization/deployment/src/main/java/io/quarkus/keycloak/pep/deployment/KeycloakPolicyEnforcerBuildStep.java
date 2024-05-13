package io.quarkus.keycloak.pep.deployment;

import java.util.function.BooleanSupplier;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerAuthorizer;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerBuildTimeConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerRecorder;
import io.quarkus.keycloak.pep.runtime.PolicyEnforcerResolver;
import io.quarkus.oidc.deployment.OidcBuildTimeConfig;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.vertx.http.deployment.RequireBodyHandlerBuildItem;
import io.quarkus.vertx.http.runtime.HttpConfiguration;

@BuildSteps(onlyIf = KeycloakPolicyEnforcerBuildStep.IsEnabled.class)
public class KeycloakPolicyEnforcerBuildStep {

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    RequireBodyHandlerBuildItem requireBody(OidcBuildTimeConfig oidcBuildTimeConfig,
            KeycloakPolicyEnforcerRecorder recorder,
            KeycloakPolicyEnforcerConfig runtimeConfig) {
        if (oidcBuildTimeConfig.enabled) {
            return new RequireBodyHandlerBuildItem(recorder.createBodyHandlerRequiredEvaluator(runtimeConfig));
        }
        return null;
    }

    @BuildStep
    public AdditionalBeanBuildItem beans(OidcBuildTimeConfig oidcBuildTimeConfig) {
        if (oidcBuildTimeConfig.enabled) {
            return AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClass(KeycloakPolicyEnforcerAuthorizer.class).build();
        }
        return null;
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.KEYCLOAK_AUTHORIZATION);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public SyntheticBeanBuildItem setup(OidcBuildTimeConfig oidcBuildTimeConfig, OidcConfig oidcRunTimeConfig,
            TlsConfig tlsConfig, KeycloakPolicyEnforcerConfig keycloakConfig, KeycloakPolicyEnforcerRecorder recorder,
            HttpConfiguration httpConfiguration) {
        if (oidcBuildTimeConfig.enabled) {
            return SyntheticBeanBuildItem.configure(PolicyEnforcerResolver.class).unremovable()
                    .types(PolicyEnforcerResolver.class)
                    .supplier(recorder.setup(oidcRunTimeConfig, keycloakConfig, tlsConfig, httpConfiguration))
                    .scope(Singleton.class)
                    .setRuntimeInit()
                    .done();
        }
        return null;
    }

    public static class IsEnabled implements BooleanSupplier {
        KeycloakPolicyEnforcerBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.policyEnforcer().enable();
        }
    }
}
