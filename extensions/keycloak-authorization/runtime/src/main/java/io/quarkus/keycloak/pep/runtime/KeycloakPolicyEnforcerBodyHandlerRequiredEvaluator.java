package io.quarkus.keycloak.pep.runtime;

import java.util.Map;
import java.util.function.BooleanSupplier;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.PathConfig;
import io.smallrye.config.SmallRyeConfig;

public final class KeycloakPolicyEnforcerBodyHandlerRequiredEvaluator implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        // right now this supplier is invoked exactly once per HTTP router finalization, so we don't need to optimize it
        KeycloakPolicyEnforcerConfig runtimeConfig = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class)
                .getConfigMapping(KeycloakPolicyEnforcerConfig.class);
        if (isBodyHandlerRequired(runtimeConfig.defaultTenant())) {
            return true;
        }
        for (KeycloakPolicyEnforcerTenantConfig tenantConfig : runtimeConfig.namedTenants().values()) {
            if (isBodyHandlerRequired(tenantConfig)) {
                return true;
            }
        }
        return false;
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
