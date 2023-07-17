package io.quarkus.spring.security.runtime.interceptor;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

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
