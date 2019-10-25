package io.quarkus.keycloak.pep;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.EnableAllSecurityServicesBuildItem;
import io.quarkus.oidc.runtime.OidcConfig;

public class KeycloakPolicyEnforcerBuildStep {

    @BuildStep
    public AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClass(KeycloakPolicyEnforcerAuthorizer.class).build();
    }

    @BuildStep
    EnableAllSecurityServicesBuildItem security() {
        return new EnableAllSecurityServicesBuildItem();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public void setup(OidcConfig oidcConfig, KeycloakPolicyEnforcerConfig config, KeycloakPolicyEnforcerRecorder recorder,
            BeanContainerBuildItem bc) {
        recorder.setup(oidcConfig, config, bc.getValue());
    }
}
