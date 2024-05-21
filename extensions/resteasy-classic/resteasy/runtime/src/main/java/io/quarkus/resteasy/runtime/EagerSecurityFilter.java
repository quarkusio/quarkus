package io.quarkus.resteasy.runtime;

import java.io.IOException;
import java.util.Map;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.arc.Arc;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.EagerSecurityInterceptorStorage;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.ext.web.RoutingContext;

@Priority(Priorities.AUTHENTICATION)
@Provider
public class EagerSecurityFilter implements ContainerRequestFilter {
    static final String SKIP_DEFAULT_CHECK = "io.quarkus.resteasy.runtime.EagerSecurityFilter#SKIP_DEFAULT_CHECK";
    private final EagerSecurityInterceptorStorage interceptorStorage;

    @Context
    ResourceInfo resourceInfo;

    @Inject
    CurrentVertxRequest currentVertxRequest;

    @Inject
    SecurityCheckStorage securityCheckStorage;

    @Inject
    CurrentIdentityAssociation identityAssociation;

    @Inject
    AuthorizationController authorizationController;

    @Inject
    JaxRsPermissionChecker jaxRsPermissionChecker;

    public EagerSecurityFilter() {
        var interceptorStorageHandle = Arc.container().instance(EagerSecurityInterceptorStorage.class);
        this.interceptorStorage = interceptorStorageHandle.isAvailable() ? interceptorStorageHandle.get() : null;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (authorizationController.isAuthorizationEnabled()) {
            var description = MethodDescription.ofMethod(resourceInfo.getResourceMethod());
            if (interceptorStorage != null) {
                applyEagerSecurityInterceptors(description);
            }
            if (jaxRsPermissionChecker.shouldRunPermissionChecks()) {
                jaxRsPermissionChecker.applyPermissionChecks();
            }
            applySecurityChecks(description);
        }
    }

    private void applySecurityChecks(MethodDescription description) {
        SecurityCheck check = securityCheckStorage.getSecurityCheck(description);
        if (check == null && securityCheckStorage.getDefaultSecurityCheck() != null
                && routingContext().get(EagerSecurityFilter.class.getName()) == null
                && routingContext().get(SKIP_DEFAULT_CHECK) == null) {
            check = securityCheckStorage.getDefaultSecurityCheck();
        }
        if (check != null) {
            if (check.isPermitAll()) {
                // add the identity only if authentication has already finished
                final SecurityIdentity identity;
                if (routingContext().user() instanceof QuarkusHttpUser user) {
                    identity = user.getSecurityIdentity();
                } else {
                    identity = null;
                }

                fireEventOnAuthZSuccess(check, identity, description);
            } else {
                if (check.requiresMethodArguments()) {
                    if (identityAssociation.getIdentity().isAnonymous()) {
                        var exception = new UnauthorizedException();
                        if (jaxRsPermissionChecker.getEventHelper().fireEventOnFailure()) {
                            fireEventOnAuthZFailure(exception, check, description);
                        }
                        throw exception;
                    }
                    // security check will be performed by CDI interceptor
                    return;
                }
                if (jaxRsPermissionChecker.getEventHelper().fireEventOnFailure()) {
                    try {
                        check.apply(identityAssociation.getIdentity(), description, null);
                    } catch (Exception e) {
                        fireEventOnAuthZFailure(e, check, description);
                        throw e;
                    }
                } else {
                    check.apply(identityAssociation.getIdentity(), description, null);
                }
                fireEventOnAuthZSuccess(check, identityAssociation.getIdentity(), description);
            }
            // prevent repeated security checks
            routingContext().put(EagerSecurityFilter.class.getName(), resourceInfo.getResourceMethod());
        }
    }

    private void fireEventOnAuthZFailure(Exception exception, SecurityCheck check, MethodDescription description) {
        jaxRsPermissionChecker.getEventHelper().fireFailureEvent(new AuthorizationFailureEvent(
                identityAssociation.getIdentity(), exception, check.getClass().getName(),
                Map.of(RoutingContext.class.getName(), routingContext()), description));
    }

    private void fireEventOnAuthZSuccess(SecurityCheck check, SecurityIdentity securityIdentity,
            MethodDescription description) {
        if (jaxRsPermissionChecker.getEventHelper().fireEventOnSuccess()) {
            jaxRsPermissionChecker.getEventHelper().fireSuccessEvent(new AuthorizationSuccessEvent(securityIdentity,
                    check.getClass().getName(), Map.of(RoutingContext.class.getName(), routingContext()), description));
        }
    }

    private RoutingContext routingContext() {
        // use actual RoutingContext (not the bean) to async events are invoked with new CDI request context
        // where the RoutingContext is not available
        return currentVertxRequest.getCurrent();
    }

    private void applyEagerSecurityInterceptors(MethodDescription description) {
        var interceptor = interceptorStorage.getInterceptor(description);
        if (interceptor != null) {
            interceptor.accept(routingContext());
        }
    }
}
