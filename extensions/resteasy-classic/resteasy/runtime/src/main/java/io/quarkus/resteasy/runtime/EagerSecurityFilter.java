package io.quarkus.resteasy.runtime;

import java.io.IOException;
import java.lang.reflect.Method;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;
import io.vertx.ext.web.RoutingContext;

@Priority(Priorities.AUTHENTICATION)
@Provider
public class EagerSecurityFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo;

    @Inject
    RoutingContext routingContext;

    @Inject
    SecurityCheckStorage securityCheckStorage;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    AuthorizationController authorizationController;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!authorizationController.isAuthorizationEnabled()) {
            return;
        }
        Method method = resourceInfo.getResourceMethod();
        SecurityCheck check = securityCheckStorage.getSecurityCheck(method);
        if (check != null) {
            if (!check.isPermitAll()) {
                if (check.requiresMethodArguments()) {
                    if (securityIdentity.isAnonymous()) {
                        throw new UnauthorizedException();
                    }
                    // security check will be performed by CDI interceptor
                    return;
                }
                check.apply(securityIdentity, method, null);
            }
            // prevent repeated security checks
            routingContext.put(EagerSecurityFilter.class.getName(), method);
        }
    }
}
