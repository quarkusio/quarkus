package io.quarkus.keycloak.pep.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.keycloak.pep.runtime.DefaultPolicyEnforcerResolver;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerAuthorizer;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerBuildTimeConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerRecorder;
import io.quarkus.oidc.deployment.OidcBuildTimeConfig;
import io.quarkus.vertx.http.deployment.RequireBodyHandlerBuildItem;

@BuildSteps(onlyIf = KeycloakPolicyEnforcerBuildStep.IsEnabled.class)
public class KeycloakPolicyEnforcerBuildStep {

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    RequireBodyHandlerBuildItem requireBody(OidcBuildTimeConfig oidcBuildTimeConfig,
            KeycloakPolicyEnforcerRecorder recorder) {
        if (oidcBuildTimeConfig.enabled()) {
            return new RequireBodyHandlerBuildItem(recorder.createBodyHandlerRequiredEvaluator());
        }
        return null;
    }

    @BuildStep
    public AdditionalBeanBuildItem beans(OidcBuildTimeConfig oidcBuildTimeConfig) {
        if (oidcBuildTimeConfig.enabled()) {
            return AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClass(KeycloakPolicyEnforcerAuthorizer.class)
                    .addBeanClass(DefaultPolicyEnforcerResolver.class)
                    .build();
        }
        return null;
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.KEYCLOAK_AUTHORIZATION);
    }

    public static class IsEnabled implements BooleanSupplier {
        KeycloakPolicyEnforcerBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.policyEnforcer().enable();
        }
    }
}
