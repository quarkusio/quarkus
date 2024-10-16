package io.quarkus.resteasy.runtime;

import static io.quarkus.resteasy.runtime.EagerSecurityFilter.SKIP_DEFAULT_CHECK;
import static io.quarkus.security.spi.runtime.SecurityHandlerConstants.EXECUTED;
import static io.quarkus.security.spi.runtime.SecurityHandlerConstants.SECURITY_HANDLER;

import java.lang.reflect.Method;

import jakarta.annotation.Priority;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * Security checks for RBAC annotations on endpoints are done by the {@link EagerSecurityFilter}, this interceptor
 * propagates the information to the SecurityHandler to prevent repeated checks. The {@link DenyAll} security check
 * is performed just once.
 */
public abstract class StandardSecurityCheckInterceptor {

    @Inject
    AuthorizationController controller;

    @Inject
    CurrentVertxRequest currentVertxRequest;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        // RoutingContext can be null if RESTEasy is used together with other stacks that do not rely on it (e.g. gRPC)
        // and this is not invoked from RESTEasy route handler
        RoutingContext routingContext = currentVertxRequest.getCurrent();
        if (controller.isAuthorizationEnabled() && routingContext != null) {
            Method method = routingContext.get(EagerSecurityFilter.class.getName());
            if (method == null) {
                // if this interceptor is run on resource method it means this is parent method for subresource
                // otherwise it would already be secured, therefore security check is applied and default JAX-RS
                // security needs to be skipped (for default security is only applied on unsecured requests)
                routingContext.put(SKIP_DEFAULT_CHECK, true);
            } else if (method.equals(ic.getMethod())) {
                ic.getContextData().put(SECURITY_HANDLER, EXECUTED);
            }
        }
        return ic.proceed();
    }

    /**
     * Prevent the SecurityHandler from performing {@link RolesAllowed} security checks
     */
    @Interceptor
    @RolesAllowed("")
    @Priority(Interceptor.Priority.LIBRARY_BEFORE - 100)
    public static final class RolesAllowedInterceptor extends StandardSecurityCheckInterceptor {

    }

    /**
     * Prevent the SecurityHandler from performing {@link PermissionsAllowed} security checks
     */
    @Interceptor
    @PermissionsAllowed("")
    @Priority(Interceptor.Priority.LIBRARY_BEFORE - 100)
    public static final class PermissionsAllowedInterceptor extends StandardSecurityCheckInterceptor {

    }

    /**
     * Prevent the SecurityHandler from performing {@link PermitAll} security checks
     */
    @Interceptor
    @PermitAll
    @Priority(Interceptor.Priority.LIBRARY_BEFORE - 100)
    public static final class PermitAllInterceptor extends StandardSecurityCheckInterceptor {

    }

    /**
     * Prevent the SecurityHandler from performing {@link Authenticated} security checks
     */
    @Interceptor
    @Authenticated
    @Priority(Interceptor.Priority.LIBRARY_BEFORE - 100)
    public static final class AuthenticatedInterceptor extends StandardSecurityCheckInterceptor {

    }
}
