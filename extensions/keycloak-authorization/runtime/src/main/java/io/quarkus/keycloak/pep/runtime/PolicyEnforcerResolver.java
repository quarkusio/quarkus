package io.quarkus.keycloak.pep.runtime;

import java.util.Map;

import org.keycloak.adapters.authorization.PolicyEnforcer;

public class PolicyEnforcerResolver {

    private final PolicyEnforcer defaultPolicyEnforcer;
    private final Map<String, PolicyEnforcer> policyEnforcerTenants;
    private final long readTimeout;

    public PolicyEnforcerResolver(PolicyEnforcer defaultPolicyEnforcer,
            Map<String, PolicyEnforcer> policyEnforcerTenants,
            final long readTimeout) {
        this.defaultPolicyEnforcer = defaultPolicyEnforcer;
        this.policyEnforcerTenants = policyEnforcerTenants;
        this.readTimeout = readTimeout;
    }

    public PolicyEnforcer getPolicyEnforcer(String tenantId) {
        return tenantId != null && policyEnforcerTenants.containsKey(tenantId)
                ? policyEnforcerTenants.get(tenantId)
                : defaultPolicyEnforcer;
    }

    public long getReadTimeout() {
        return readTimeout;
    }
}
