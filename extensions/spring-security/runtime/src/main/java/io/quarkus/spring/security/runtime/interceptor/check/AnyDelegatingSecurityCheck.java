package io.quarkus.spring.security.runtime.interceptor.check;

import java.lang.reflect.Method;
import java.util.List;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;

/**
 * A {@link SecurityCheck} where if any of the delegates passes the security check then
 * the delegate passes as well
 */
public class AnyDelegatingSecurityCheck implements SecurityCheck {

    private final List<SecurityCheck> securityChecks;

    public AnyDelegatingSecurityCheck(List<SecurityCheck> securityChecks) {
        this.securityChecks = securityChecks;
    }

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
        SecurityException thrownException = null;
        for (int i = 0; i < securityChecks.size(); i++) {
            try {
                securityChecks.get(i).apply(identity, method, parameters);
                // no exception was thrown so we can just return
                return;
            } catch (SecurityException e) {
                thrownException = e;
            }
        }
        if (thrownException != null) {
            throw thrownException;
        }
    }

    @Override
    public void apply(SecurityIdentity identity, MethodDescription methodDescription, Object[] parameters) {
        SecurityException thrownException = null;
        for (int i = 0; i < securityChecks.size(); i++) {
            try {
                securityChecks.get(i).apply(identity, methodDescription, parameters);
                // no exception was thrown so we can just return
                return;
            } catch (SecurityException e) {
                thrownException = e;
            }
        }
        if (thrownException != null) {
            throw thrownException;
        }
    }
}
