package io.quarkus.security.runtime.interceptor;

import jakarta.annotation.Priority;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.security.spi.runtime.AuthorizationController;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Interceptor
@RolesAllowed("")
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class RolesAllowedInterceptor {

    @Inject
    SecurityHandler handler;

    @Inject
    AuthorizationController controller;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        if (controller.isAuthorizationEnabled()) {
            return handler.handle(ic);
        } else {
            return ic.proceed();
        }
    }
}
