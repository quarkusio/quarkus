package io.quarkus.resteasy.runtime;

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
        if (controller.isAuthorizationEnabled() && currentVertxRequest.getCurrent() != null) {
            Method method = currentVertxRequest.getCurrent().get(EagerSecurityFilter.class.getName());
            if (method != null && method.equals(ic.getMethod())) {
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
    @Priority(Interceptor.Priority.PLATFORM_BEFORE)
    public static final class RolesAllowedInterceptor extends StandardSecurityCheckInterceptor {

    }

    /**
     * Prevent the SecurityHandler from performing {@link PermissionsAllowed} security checks
     */
    @Interceptor
    @PermissionsAllowed("")
    @Priority(Interceptor.Priority.PLATFORM_BEFORE)
    public static final class PermissionsAllowedInterceptor extends StandardSecurityCheckInterceptor {

    }

    /**
     * Prevent the SecurityHandler from performing {@link PermitAll} security checks
     */
    @Interceptor
    @PermitAll
    @Priority(Interceptor.Priority.PLATFORM_BEFORE)
    public static final class PermitAllInterceptor extends StandardSecurityCheckInterceptor {

    }

    /**
     * Prevent the SecurityHandler from performing {@link Authenticated} security checks
     */
    @Interceptor
    @Authenticated
    @Priority(Interceptor.Priority.PLATFORM_BEFORE)
    public static final class AuthenticatedInterceptor extends StandardSecurityCheckInterceptor {

    }
}
