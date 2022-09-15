package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.oidc.runtime.OidcUtils;
import io.vertx.ext.web.RoutingContext;

@Interceptor
@HrTenant
public class HrInterceptor {
    @Inject
    RoutingContext routingContext;

    @AroundInvoke
    Object setTenant(InvocationContext context) throws Exception {
        routingContext.put(OidcUtils.TENANT_ID_ATTRIBUTE, "hr");
        return context.proceed();
    }
}
