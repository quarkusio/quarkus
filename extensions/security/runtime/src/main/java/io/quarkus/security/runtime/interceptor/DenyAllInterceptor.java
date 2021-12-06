package io.quarkus.security.runtime.interceptor;

import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.quarkus.security.spi.runtime.AuthorizationController;

/**
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Interceptor
@DenyAll
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
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
