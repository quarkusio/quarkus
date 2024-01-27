package io.quarkus.resteasy.runtime;

import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_FAILURE;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_SUCCESS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.ConfigProvider;

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
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.quarkus.vertx.http.runtime.security.EagerSecurityInterceptorStorage;
import io.vertx.ext.web.RoutingContext;

@Priority(Priorities.AUTHENTICATION)
@Provider
public class EagerSecurityFilter implements ContainerRequestFilter {

    private static final Consumer<RoutingContext> NULL_SENTINEL = new Consumer<RoutingContext>() {
        @Override
        public void accept(RoutingContext routingContext) {

        }
    };
    private final Map<MethodDescription, Consumer<RoutingContext>> cache = new HashMap<>();
    private final EagerSecurityInterceptorStorage interceptorStorage;
    private final SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> eventHelper;

    @Context
    ResourceInfo resourceInfo;

    @Inject
    RoutingContext routingContext;

    @Inject
    SecurityCheckStorage securityCheckStorage;

    @Inject
    CurrentIdentityAssociation identityAssociation;

    @Inject
    AuthorizationController authorizationController;

    public EagerSecurityFilter() {
        var interceptorStorageHandle = Arc.container().instance(EagerSecurityInterceptorStorage.class);
        this.interceptorStorage = interceptorStorageHandle.isAvailable() ? interceptorStorageHandle.get() : null;
        Event<Object> event = Arc.container().beanManager().getEvent();
        this.eventHelper = new SecurityEventHelper<>(event.select(AuthorizationSuccessEvent.class),
                event.select(AuthorizationFailureEvent.class), AUTHORIZATION_SUCCESS,
                AUTHORIZATION_FAILURE, Arc.container().beanManager(),
                ConfigProvider.getConfig().getOptionalValue("quarkus.security.events.enabled", Boolean.class).orElse(false));
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (authorizationController.isAuthorizationEnabled()) {
            var description = MethodDescription.ofMethod(resourceInfo.getResourceMethod());
            if (interceptorStorage != null) {
                applyEagerSecurityInterceptors(description);
            }
            applySecurityChecks(description);
        }
    }

    private void applySecurityChecks(MethodDescription description) {
        SecurityCheck check = securityCheckStorage.getSecurityCheck(description);
        if (check != null) {
            if (check.isPermitAll()) {
                fireEventOnAuthZSuccess(check, null);
            } else {
                if (check.requiresMethodArguments()) {
                    if (identityAssociation.getIdentity().isAnonymous()) {
                        var exception = new UnauthorizedException();
                        if (eventHelper.fireEventOnFailure()) {
                            fireEventOnAuthZFailure(exception, check);
                        }
                        throw exception;
                    }
                    // security check will be performed by CDI interceptor
                    return;
                }
                if (eventHelper.fireEventOnFailure()) {
                    try {
                        check.apply(identityAssociation.getIdentity(), description, null);
                    } catch (Exception e) {
                        fireEventOnAuthZFailure(e, check);
                        throw e;
                    }
                } else {
                    check.apply(identityAssociation.getIdentity(), description, null);
                }
                fireEventOnAuthZSuccess(check, identityAssociation.getIdentity());
            }
            // prevent repeated security checks
            routingContext.put(EagerSecurityFilter.class.getName(), resourceInfo.getResourceMethod());
        }
    }

    private void fireEventOnAuthZFailure(Exception exception, SecurityCheck check) {
        eventHelper.fireFailureEvent(new AuthorizationFailureEvent(
                identityAssociation.getIdentity(), exception, check.getClass().getName(),
                Map.of(RoutingContext.class.getName(), routingContext)));
    }

    private void fireEventOnAuthZSuccess(SecurityCheck check, SecurityIdentity securityIdentity) {
        if (eventHelper.fireEventOnSuccess()) {
            eventHelper.fireSuccessEvent(new AuthorizationSuccessEvent(securityIdentity,
                    check.getClass().getName(), Map.of(RoutingContext.class.getName(), routingContext)));
        }
    }

    private void applyEagerSecurityInterceptors(MethodDescription description) {
        var interceptor = cache.get(description);
        if (interceptor != NULL_SENTINEL) {
            if (interceptor != null) {
                interceptor.accept(routingContext);
            } else {
                interceptor = interceptorStorage.getInterceptor(description);
                if (interceptor == null) {
                    cache.put(description, NULL_SENTINEL);
                } else {
                    cache.put(description, interceptor);
                    interceptor.accept(routingContext);
                }
            }
        }
    }
}
