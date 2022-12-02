package io.quarkus.security.spi.runtime;

import javax.inject.Singleton;

/**
 * controller that allows authorization to be disabled in tests.
 */
@Singleton
public class AuthorizationController {

    public boolean isAuthorizationEnabled() {
        return true;
    }
}
