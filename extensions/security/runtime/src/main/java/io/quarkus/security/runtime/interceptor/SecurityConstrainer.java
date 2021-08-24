package io.quarkus.security.runtime.interceptor;

import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Singleton
public class SecurityConstrainer {

    @Inject
    SecurityIdentity identity;

    @Inject
    SecurityCheckStorage storage;

    public void check(Method method, Object[] parameters) {

        SecurityCheck securityCheck = storage.getSecurityCheck(method);
        if (securityCheck != null) {
            securityCheck.apply(identity, method, parameters);
        }
    }
}
