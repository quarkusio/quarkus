package io.quarkus.oidc.runtime.trace;

import jakarta.enterprise.inject.Instance;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.runtime.OidcUtils;

public class ObservabilityRequestResponseFilter implements OidcRequestFilter, OidcResponseFilter {

    private Instance<Tracer> tracer;

    public ObservabilityRequestResponseFilter(Instance<Tracer> tracer) {
        this.tracer = tracer;
    }

    @Override
    public void filter(OidcRequestContext requestContext) {

        if (tracer.isResolvable()) {
            String operationName = getOperationName(requestContext);
            Span span = tracer.get().spanBuilder(operationName).startSpan();
            Scope scope = span.makeCurrent();
            requestContext.contextProperties().put(Span.class.getName(), span);
            requestContext.contextProperties().put(Scope.class.getName(), scope);
        }
    }

    @Override
    public void filter(OidcResponseContext responseContext) {
        if (tracer.isResolvable()) {
            Span span = responseContext.requestProperties().get(Span.class.getName(), Span.class);
            span.end();
        }
    }

    private static String getOperationName(OidcRequestContext requestContext) {
        String tenantId = requestContext.contextProperties().get(OidcUtils.TENANT_ID_ATTRIBUTE);
        String tenantSuffix = OidcUtils.DEFAULT_TENANT_ID.equals(tenantId) ? "." : "." + tenantId + ".";
        String operation = requestContext.contextProperties().get(OidcUtils.OIDC_OPERATION);
        return "quarkus.oidc" + tenantSuffix + (operation == null ? "" : operation);
    }
}
