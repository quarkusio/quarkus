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

import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.interception.jaxrs.PostMatchContainerRequestContext;
import org.jboss.resteasy.spi.ResourceFactory;

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
            var description = createResourceMethodDescription(requestContext, resourceInfo);

            if (interceptorStorage != null) {
                applyEagerSecurityInterceptors(description);
            }
            var authZPolicyMethod = jaxRsPermissionChecker.getMethodSecuredWithAuthZPolicy(description.invokedMethodDesc(),
                    description.fallbackMethodDesc());
            if (jaxRsPermissionChecker.shouldRunPermissionChecks()) {
                jaxRsPermissionChecker.applyPermissionChecks(authZPolicyMethod);
            }

            if (authZPolicyMethod == null) { // if we didn't run check for @AuthorizationPolicy
                applySecurityChecks(description);
            }
        }
    }

    private void applySecurityChecks(ResourceMethodDescription resourceMethodDescription) {
        var description = resourceMethodDescription.invokedMethodDesc();
        SecurityCheck check = securityCheckStorage.getSecurityCheck(description);
        if (check == null && resourceMethodDescription.fallbackMethodDesc() != null) {
            description = resourceMethodDescription.fallbackMethodDesc();
            check = securityCheckStorage.getSecurityCheck(description);
        }

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

    private void applyEagerSecurityInterceptors(ResourceMethodDescription description) {
        var interceptor = interceptorStorage.getInterceptor(description.invokedMethodDesc());
        if (description.fallbackMethodDesc() != null && interceptor == null) {
            interceptor = interceptorStorage.getInterceptor(description.fallbackMethodDesc());
        }
        if (interceptor != null) {
            interceptor.accept(routingContext());
        }
    }

    private static Class<?> getScannableClass(ResourceMethodInvoker invoker) {
        var resourceFactory = getResourceFactory(invoker);
        return resourceFactory == null ? null : resourceFactory.getScannableClass();
    }

    private static ResourceMethodDescription createResourceMethodDescription(ContainerRequestContext requestContext,
            ResourceInfo resourceInfo) {
        var resourceMethod = resourceInfo.getResourceMethod();
        var resourceInfoDesc = MethodDescription.ofMethod(resourceMethod);

        // declaring class in ResourceInfo method commonly matches class with @Path rather than actually invoked class
        // until RESTEasy introduces new method like 'getInvokedMethod' we need to infer method that is actually invoked
        if (requestContext instanceof PostMatchContainerRequestContext postMatchRequestContext) {
            var scannableClass = getScannableClass(postMatchRequestContext.getResourceMethod());
            if (scannableClass != null && !resourceMethod.getDeclaringClass().equals(scannableClass)) {
                try {
                    var method = scannableClass.getMethod(resourceMethod.getName(), resourceMethod.getParameterTypes());
                    var scannableClassDesc = MethodDescription.ofMethod(method);

                    if (!scannableClassDesc.equals(resourceInfoDesc)) {
                        return new ResourceMethodDescription(scannableClassDesc, resourceInfoDesc);
                    }
                } catch (NoSuchMethodException e) {
                    // do nothing
                }
            }
        }

        return new ResourceMethodDescription(resourceInfoDesc, null);
    }

    static native ResourceFactory getResourceFactory(ResourceMethodInvoker invoker);

    /**
     * @param invokedMethodDesc description of actually invoked method (method on which CDI interceptors are applied)
     * @param fallbackMethodDesc description that we used in the past; not null when different to {@code invokedMethodDesc}
     */
    private record ResourceMethodDescription(MethodDescription invokedMethodDesc, MethodDescription fallbackMethodDesc) {

    }
}
