package io.quarkus.it.keycloak;

import jakarta.interceptor.Interceptor;

import io.quarkus.oidc.TenantResolverInterceptor;

@HrTenant
@Interceptor
public class HrInterceptor extends TenantResolverInterceptor {

    @Override
    protected String getTenantId() {
        return "hr";
    }

}
