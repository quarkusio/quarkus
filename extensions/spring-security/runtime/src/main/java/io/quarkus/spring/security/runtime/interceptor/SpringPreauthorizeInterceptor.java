package io.quarkus.spring.security.runtime.interceptor;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.springframework.security.access.prepost.PreAuthorize;

import io.quarkus.security.runtime.interceptor.SecurityHandler;
import io.quarkus.security.spi.runtime.AuthorizationController;

@Interceptor
@PreAuthorize("")
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class SpringPreauthorizeInterceptor {

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
