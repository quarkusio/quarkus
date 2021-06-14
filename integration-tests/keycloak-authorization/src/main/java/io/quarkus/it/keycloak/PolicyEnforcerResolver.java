package io.quarkus.it.keycloak;

import java.util.Collections;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;

import io.quarkus.keycloak.pep.PolicyEnforcerConfigResolver;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class PolicyEnforcerResolver implements PolicyEnforcerConfigResolver {
    @Override
    public KeycloakPolicyEnforcerTenantConfig resolve(RoutingContext context) {
        if (context.request().path().endsWith("dynamic")) {
            KeycloakPolicyEnforcerTenantConfig config = new KeycloakPolicyEnforcerTenantConfig();
            config.policyEnforcer.enforcementMode = PolicyEnforcerConfig.EnforcementMode.ENFORCING;

            KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.PathConfig permissionDynamicPath = new KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.PathConfig();
            permissionDynamicPath.path = Optional.of("/api-permission-dynamic");
            permissionDynamicPath.name = Optional.of("Permission Resource Dynamic Tenant");
            permissionDynamicPath.methods = Collections.emptyMap();

            KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.ClaimInformationPointConfig cipc = new KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.ClaimInformationPointConfig();
            cipc.simpleConfig = Collections.singletonMap("claims", Collections.singletonMap("static-claim", "static-claim"));
            cipc.complexConfig = Collections.emptyMap();
            permissionDynamicPath.claimInformationPoint = cipc;

            KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.ClaimInformationPointConfig cipc2 = new KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.ClaimInformationPointConfig();
            cipc2.simpleConfig = Collections.emptyMap();
            cipc2.complexConfig = Collections.emptyMap();
            config.policyEnforcer.claimInformationPoint = cipc2;

            config.policyEnforcer.paths = Collections.singletonMap("1", permissionDynamicPath);

            return config;
        }
        return null;
    }
}
