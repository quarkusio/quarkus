package io.quarkus.security.runtime.interceptor;

import static io.quarkus.security.spi.runtime.SecurityHandlerConstants.SECURITY_INTERCEPTOR_PRIORITY;

import jakarta.annotation.Priority;
import jakarta.annotation.security.DenyAll;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.security.spi.runtime.AuthorizationController;

/**
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Interceptor
@DenyAll
@Priority(SECURITY_INTERCEPTOR_PRIORITY)
public class DenyAllInterceptor {

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
