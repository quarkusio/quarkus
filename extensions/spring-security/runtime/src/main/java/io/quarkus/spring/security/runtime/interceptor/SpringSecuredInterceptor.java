package io.quarkus.spring.security.runtime.interceptor;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.springframework.security.access.annotation.Secured;

import io.quarkus.security.runtime.interceptor.SecurityHandler;
import io.quarkus.security.spi.runtime.AuthorizationController;

@Interceptor
@Secured("")
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class SpringSecuredInterceptor {

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
