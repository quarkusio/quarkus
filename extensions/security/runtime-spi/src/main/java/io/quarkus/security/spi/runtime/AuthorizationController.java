package io.quarkus.security.spi.runtime;

import javax.inject.Singleton;

import io.vertx.ext.web.RoutingContext;

/**
 * controller that allows authorization to be disabled in tests.
 */
@Singleton
public class AuthorizationController {

    public boolean isAuthorizationEnabled() {
        return true;
    }

    /** Overwrite, if you require routingContext information on HttpAuthorizer#checkPermission processing */
    public boolean isAuthorizationEnabled(RoutingContext routingContext) {
        return isAuthorizationEnabled();
    }
}
