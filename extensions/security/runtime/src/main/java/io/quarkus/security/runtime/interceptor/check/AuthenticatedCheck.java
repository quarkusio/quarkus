package io.quarkus.security.runtime.interceptor.check;

import java.lang.reflect.Method;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.spi.runtime.SecurityCheck;

public class AuthenticatedCheck implements SecurityCheck {

    public static final AuthenticatedCheck INSTANCE = new AuthenticatedCheck();

    private volatile AuthorizationController authorizationController;

    private AuthenticatedCheck() {
    }

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
        if (isAuthorizationDisabled()) {
            return;
        }
        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        }
    }

    private boolean isAuthorizationDisabled() {
        if (authorizationController != null) {
            return !authorizationController.isAuthorizationEnabled();
        }

        ArcContainer container = Arc.container();
        if ((container == null) || !container.isRunning()) {
            return false;
        }
        InstanceHandle<AuthorizationController> instance = container.instance(AuthorizationController.class);
        if (instance.isAvailable()) {
            authorizationController = instance.get();
            return !instance.get().isAuthorizationEnabled();
        }
        return false;
    }
}
