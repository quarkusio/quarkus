package io.quarkus.keycloak.pep.deployment;

import java.util.Map;
import java.util.function.BooleanSupplier;

import javax.inject.Singleton;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.keycloak.pep.runtime.KeycloakPoilcyEnforcerBuildTimeConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerAuthorizer;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerRecorder;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.PathConfig;
import io.quarkus.keycloak.pep.runtime.PolicyEnforcerResolver;
import io.quarkus.oidc.deployment.OidcBuildTimeConfig;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.vertx.http.deployment.RequireBodyHandlerBuildItem;
import io.quarkus.vertx.http.runtime.HttpConfiguration;

public class KeycloakPolicyEnforcerBuildStep {

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.KEYCLOAK_AUTHORIZATION);
    }

    @BuildStep(onlyIf = IsEnabled.class)
    RequireBodyHandlerBuildItem requireBody(OidcBuildTimeConfig oidcBuildTimeConfig, KeycloakPolicyEnforcerConfig config) {
        if (oidcBuildTimeConfig.enabled) {
            if (isBodyHandlerRequired(config.defaultTenant)) {
                return new RequireBodyHandlerBuildItem();
            }
            for (KeycloakPolicyEnforcerTenantConfig tenantConfig : config.namedTenants.values()) {
                if (isBodyHandlerRequired(tenantConfig)) {
                    return new RequireBodyHandlerBuildItem();
                }
            }
        }
        return null;
    }

    private static boolean isBodyHandlerRequired(KeycloakPolicyEnforcerTenantConfig config) {
        if (isBodyClaimInformationPointDefined(config.policyEnforcer.claimInformationPoint.simpleConfig)) {
            return true;
        }
        for (PathConfig path : config.policyEnforcer.paths.values()) {
            if (isBodyClaimInformationPointDefined(path.claimInformationPoint.simpleConfig)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBodyClaimInformationPointDefined(Map<String, Map<String, String>> claims) {
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

    @BuildStep(onlyIf = IsEnabled.class)
    public AdditionalBeanBuildItem beans(OidcBuildTimeConfig oidcBuildTimeConfig, KeycloakPolicyEnforcerConfig config) {
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
    @BuildStep(onlyIf = IsEnabled.class)
    public SyntheticBeanBuildItem setup(OidcBuildTimeConfig oidcBuildTimeConfig, OidcConfig oidcRunTimeConfig,
            TlsConfig tlsConfig,
            KeycloakPolicyEnforcerConfig keycloakConfig, KeycloakPolicyEnforcerRecorder recorder,
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
        KeycloakPoilcyEnforcerBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.policyEnforcer.enable;
        }
    }
}
