package io.quarkus.spring.security.runtime.interceptor.check;

import java.lang.reflect.Method;
import java.util.List;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.SecurityCheck;

/**
 * A {@link SecurityCheck} where all delegates must pass
 */
public class AllDelegatingSecurityCheck implements SecurityCheck {

    private final List<SecurityCheck> securityChecks;

    public AllDelegatingSecurityCheck(List<SecurityCheck> securityChecks) {
        this.securityChecks = securityChecks;
    }

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
        for (SecurityCheck securityCheck : securityChecks) {
            securityCheck.apply(identity, method, parameters);
        }
    }
}
