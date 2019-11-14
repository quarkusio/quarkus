package io.quarkus.keycloak.pep.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.EnableAllSecurityServicesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerAuthorizer;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerRecorder;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.runtime.OidcConfig;

public class KeycloakPolicyEnforcerBuildStep {

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(FeatureBuildItem.KEYCLOAK_AUTHORIZATION);
    }

    @BuildStep
    public AdditionalBeanBuildItem beans(KeycloakPolicyEnforcerConfig config) {
        if (config.policyEnforcer.enable) {
            return AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClass(KeycloakPolicyEnforcerAuthorizer.class).build();
        }
        return null;
    }

    @BuildStep
    EnableAllSecurityServicesBuildItem security() {
        return new EnableAllSecurityServicesBuildItem();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public void setup(OidcConfig oidcConfig, KeycloakPolicyEnforcerConfig config, KeycloakPolicyEnforcerRecorder recorder,
            BeanContainerBuildItem bc) {
        if (!oidcConfig.getApplicationType().equals(OidcConfig.ApplicationType.SERVICE)) {
            throw new OIDCException("Application type [" + oidcConfig.getApplicationType() + "] not supported");
        }
        if (config.policyEnforcer.enable) {
            recorder.setup(oidcConfig, config, bc.getValue());
        }
    }
}
