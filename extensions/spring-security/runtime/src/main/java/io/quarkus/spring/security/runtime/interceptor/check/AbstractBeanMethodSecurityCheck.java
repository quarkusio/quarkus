package io.quarkus.spring.security.runtime.interceptor.check;

import java.lang.reflect.Method;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.SecurityCheck;

/**
 * Implementations of this class are generated for expressions in @PreAuthorize that
 * invoke a method of a bean
 */
public abstract class AbstractBeanMethodSecurityCheck implements SecurityCheck {

    protected abstract boolean check(SecurityIdentity identity, Object[] parameters);

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
        if (check(identity, parameters)) {
            return;
        }
        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        } else {
            throw new ForbiddenException();
        }
    }

}
