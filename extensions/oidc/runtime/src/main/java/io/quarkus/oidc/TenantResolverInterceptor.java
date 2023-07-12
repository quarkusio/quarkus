package io.quarkus.oidc;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import io.quarkus.oidc.runtime.OidcUtils;
import io.vertx.ext.web.RoutingContext;

public abstract class TenantResolverInterceptor {
    @Inject
    RoutingContext routingContext;

    @AroundInvoke
    public Object setTenant(InvocationContext context) throws Exception {
        routingContext.put(OidcUtils.TENANT_ID_ATTRIBUTE, getTenantId());
        return context.proceed();
    }

    protected abstract String getTenantId();
}
