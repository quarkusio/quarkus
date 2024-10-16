package io.quarkus.vertx.http.runtime.security;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.security.spi.runtime.MethodDescription;

/**
 * Quarkus generates this storage of endpoints secured with {@link io.quarkus.vertx.http.security.AuthorizationPolicy}.
 * The storage can be retrieved from CDI container when the Quarkus Security extension is present.
 */
public abstract class AuthorizationPolicyStorage {

    protected AuthorizationPolicyStorage() {
    }

    protected abstract Map<MethodDescription, String> getMethodToPolicyName();

    /**
     * @param securedMethodDesc method description
     * @return true if method is secured with {@link io.quarkus.vertx.http.security.AuthorizationPolicy}
     */
    public boolean requiresAuthorizationPolicy(MethodDescription securedMethodDesc) {
        if (securedMethodDesc == null) {
            return false;
        }
        return getMethodToPolicyName().containsKey(securedMethodDesc);
    }

    // used by generated subclass
    public final static class MethodsToPolicyBuilder {

        private final Map<MethodDescription, String> methodToPolicyName = new HashMap<>();

        public MethodsToPolicyBuilder() {
        }

        public MethodsToPolicyBuilder addMethodToPolicyName(String policyName, String className, String methodName,
                String[] parameterTypes) {
            methodToPolicyName.put(new MethodDescription(className, methodName, parameterTypes), policyName);
            return this;
        }

        public Map<MethodDescription, String> build() {
            return Map.copyOf(methodToPolicyName);
        }
    }
}
