package io.quarkus.keycloak.pep.runtime;

import java.util.Map;

public class PolicyEnforcerResolver {

    private final QuarkusPolicyEnforcer defaultPolicyEnforcer;
    private final Map<String, QuarkusPolicyEnforcer> policyEnforcerTenants;
    private final long readTimeout;

    public PolicyEnforcerResolver(QuarkusPolicyEnforcer defaultPolicyEnforcer,
            Map<String, QuarkusPolicyEnforcer> policyEnforcerTenants,
            final long readTimeout) {
        this.defaultPolicyEnforcer = defaultPolicyEnforcer;
        this.policyEnforcerTenants = policyEnforcerTenants;
        this.readTimeout = readTimeout;
    }

    public QuarkusPolicyEnforcer getPolicyEnforcer(String tenantId) {
        return tenantId != null && policyEnforcerTenants.containsKey(tenantId)
                ? policyEnforcerTenants.get(tenantId)
                : defaultPolicyEnforcer;
    }

    public long getReadTimeout() {
        return readTimeout;
    }
}
