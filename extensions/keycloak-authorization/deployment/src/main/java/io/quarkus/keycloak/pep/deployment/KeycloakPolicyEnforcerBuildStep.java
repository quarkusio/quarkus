package io.quarkus.keycloak.pep.deployment;

import java.util.Map;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.EnableAllSecurityServicesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerAuthorizer;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerRecorder;
import io.quarkus.oidc.runtime.OidcBuildTimeConfig;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.vertx.http.deployment.RequireBodyHandlerBuildItem;
import io.quarkus.vertx.http.runtime.HttpConfiguration;

public class KeycloakPolicyEnforcerBuildStep {

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.KEYCLOAK_AUTHORIZATION);
    }

    @BuildStep
    RequireBodyHandlerBuildItem requireBody(KeycloakPolicyEnforcerConfig config) {
        if (config.policyEnforcer.enable) {
            if (isBodyClaimInformationPointDefined(config.policyEnforcer.claimInformationPoint.simpleConfig)) {
                return new RequireBodyHandlerBuildItem();
            }
            for (KeycloakPolicyEnforcerConfig.KeycloakConfigPolicyEnforcer.PathConfig path : config.policyEnforcer.paths
                    .values()) {
                if (isBodyClaimInformationPointDefined(path.claimInformationPoint.simpleConfig)) {
                    return new RequireBodyHandlerBuildItem();
                }
            }
        }
        return null;
    }

    private boolean isBodyClaimInformationPointDefined(Map<String, Map<String, String>> claims) {
        for (Map.Entry<String, Map<String, String>> entry : claims.entrySet()) {
            Map<String, String> value = entry.getValue();

            for (String nestedValue : value.values()) {
                if (nestedValue.contains("request.body")) {
                    return true;
                }
            }
        }

        return false;
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
    public void setup(OidcBuildTimeConfig oidcBuildTimeConfig, OidcConfig oidcRunTimeConfig, TlsConfig tlsConfig,
            KeycloakPolicyEnforcerConfig keycloakConfig, KeycloakPolicyEnforcerRecorder recorder, BeanContainerBuildItem bc,
            HttpConfiguration httpConfiguration) {
        if (oidcBuildTimeConfig.enabled && keycloakConfig.policyEnforcer.enable) {
            recorder.setup(oidcRunTimeConfig, keycloakConfig, tlsConfig, bc.getValue(), httpConfiguration);
        }
    }
}
