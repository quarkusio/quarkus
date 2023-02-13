package io.quarkus.resteasy.reactive.server.runtime;

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

import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;

import io.quarkus.security.Authenticated;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.spi.runtime.MethodDescription;

/**
 * Security checks for RBAC annotations on endpoints are done by
 * the {@link io.quarkus.resteasy.reactive.server.runtime.security.EagerSecurityHandler},
 * this interceptor propagates the information to the SecurityHandler to prevent repeated checks. The {@link DenyAll}
 * security check is performed just once.
 */
public abstract class StandardSecurityCheckInterceptor {

    public static final String STANDARD_SECURITY_CHECK_INTERCEPTOR = StandardSecurityCheckInterceptor.class.getName();

    @Inject
    AuthorizationController controller;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        if (controller.isAuthorizationEnabled() && CurrentRequestManager.get() != null
                && alreadyDoneByEagerSecurityHandler(
                        CurrentRequestManager.get().getProperty(STANDARD_SECURITY_CHECK_INTERCEPTOR), ic.getMethod())) {
            ic.getContextData().put(SECURITY_HANDLER, EXECUTED);
        }
        return ic.proceed();
    }

    private boolean alreadyDoneByEagerSecurityHandler(Object methodWithFinishedChecks, Method method) {
        // compare methods: EagerSecurityHandler only intercept endpoints, we still want SecurityHandler run for CDI beans
        return methodWithFinishedChecks != null && MethodDescription.ofMethod(method).equals(methodWithFinishedChecks);
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
     * Prevent the SecurityHandler from performing {@link jakarta.annotation.security.PermitAll} security checks
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
