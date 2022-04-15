package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
