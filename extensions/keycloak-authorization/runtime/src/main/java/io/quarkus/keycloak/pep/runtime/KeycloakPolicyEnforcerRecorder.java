package io.quarkus.keycloak.pep.runtime;

import java.util.Map;
import java.util.function.BooleanSupplier;

import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.PathConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KeycloakPolicyEnforcerRecorder {

    public BooleanSupplier createBodyHandlerRequiredEvaluator(KeycloakPolicyEnforcerConfig config) {
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                if (isBodyHandlerRequired(config.defaultTenant())) {
                    return true;
                }
                for (KeycloakPolicyEnforcerTenantConfig tenantConfig : config.namedTenants().values()) {
                    if (isBodyHandlerRequired(tenantConfig)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private static boolean isBodyHandlerRequired(KeycloakPolicyEnforcerTenantConfig config) {
        if (isBodyClaimInformationPointDefined(config.policyEnforcer().claimInformationPoint().simpleConfig())) {
            return true;
        }
        for (PathConfig path : config.policyEnforcer().paths().values()) {
            if (isBodyClaimInformationPointDefined(path.claimInformationPoint().simpleConfig())) {
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

}
