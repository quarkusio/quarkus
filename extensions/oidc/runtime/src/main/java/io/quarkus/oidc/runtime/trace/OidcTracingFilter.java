package io.quarkus.oidc.runtime.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.runtime.OidcUtils;

public class OidcTracingFilter implements OidcRequestFilter {

    public OidcTracingFilter() {
    }

    @Override
    public void filter(OidcRequestContext requestContext) {

        Span localRootSpan = LocalRootSpan.current();
        String operation = requestContext.contextProperties().get(OidcUtils.OIDC_OPERATION);
        localRootSpan.updateName(operation);

        localRootSpan.setAttribute("oidc.operation", getOperationName(requestContext));
    }

    private static String getOperationName(OidcRequestContext requestContext) {
        String tenantId = requestContext.contextProperties().get(OidcUtils.TENANT_ID_ATTRIBUTE);
        String tenantSuffix = OidcUtils.DEFAULT_TENANT_ID.equals(tenantId) ? "." : "." + tenantId + ".";
        String operation = requestContext.contextProperties().get(OidcUtils.OIDC_OPERATION);
        return "quarkus.oidc" + tenantSuffix + (operation == null ? "" : operation);
    }
}
