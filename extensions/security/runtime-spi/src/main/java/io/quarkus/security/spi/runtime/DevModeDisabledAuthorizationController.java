package io.quarkus.security.spi.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import io.quarkus.runtime.LaunchMode;

/**
 * Controller used in dev mode if {@code quarkus.security.auth.enabled-in-dev-mode=false}.
 */
@Alternative
@Priority(Interceptor.Priority.LIBRARY_AFTER)
@Singleton
public final class DevModeDisabledAuthorizationController extends AuthorizationController {

    public boolean isAuthorizationEnabled() {
        if (LaunchMode.current() != LaunchMode.DEVELOPMENT) {
            throw new IllegalStateException("This implementation is only available in dev mode");
        }
        return false;
    }
}
