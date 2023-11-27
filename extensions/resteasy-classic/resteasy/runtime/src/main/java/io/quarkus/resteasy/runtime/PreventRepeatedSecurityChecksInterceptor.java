package io.quarkus.resteasy.runtime;

import static io.quarkus.security.spi.runtime.SecurityHandlerConstants.EXECUTED;
import static io.quarkus.security.spi.runtime.SecurityHandlerConstants.SECURITY_HANDLER;

import java.lang.reflect.Method;

import jakarta.annotation.Priority;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.vertx.ext.web.RoutingContext;

@Interceptor
@RolesAllowed("")
@PermissionsAllowed("")
@PermitAll
@Authenticated
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class PreventRepeatedSecurityChecksInterceptor {

    @Inject
    AuthorizationController controller;

    @Inject
    RoutingContext routingContext;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        if (controller.isAuthorizationEnabled()) {
            Method method = routingContext.get(EagerSecurityFilter.class.getName());
            if (method != null && method.equals(ic.getMethod())) {
                ic.getContextData().put(SECURITY_HANDLER, EXECUTED);
            }
        }
        return ic.proceed();
    }
}
