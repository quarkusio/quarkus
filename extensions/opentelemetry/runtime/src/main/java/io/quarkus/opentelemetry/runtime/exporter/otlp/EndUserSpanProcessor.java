package io.quarkus.opentelemetry.runtime.exporter.otlp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.semconv.SemanticAttributes;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.SecurityIdentity;

@ApplicationScoped
public class EndUserSpanProcessor implements SpanProcessor {

    private static final Logger LOG = Logger.getLogger(EndUserSpanProcessor.class);

    @Inject
    protected SecurityIdentity securityIdentity;

    @Inject
    protected ManagedExecutor managedExecutor;

    @Override
    @ActivateRequestContext
    public void onStart(Context parentContext, ReadWriteSpan span) {
        managedExecutor.execute(
                () -> {
                    try {
                        if (!span.isRecording()) {
                            LOG.debug("Failed to set end user attributes on spanId:" +
                                    span.getSpanContext().getSpanId() + " Span not recording");
                            return;
                        }
                        ManagedContext managedContext = Arc.container().requestContext();
                        if (!managedContext.isActive()) {
                            LOG.debug("Failed to set end user attributes on span.spanId:" +
                                    span.getSpanContext().getSpanId() + " Request Context not active");
                            return;
                        }
                        span.setAllAttributes(
                                securityIdentity.isAnonymous()
                                        ? Attributes.empty()
                                        : Attributes.of(
                                                SemanticAttributes.ENDUSER_ID,
                                                securityIdentity.getPrincipal().getName(),
                                                SemanticAttributes.ENDUSER_ROLE,
                                                securityIdentity.getRoles().toString()));
                    } catch (Exception e) {
                        LOG.debug("Failed to set end user attributes on span", e);
                    }
                });
    }

    @Override
    public boolean isStartRequired() {
        return Boolean.TRUE;
    }

    @Override
    public void onEnd(ReadableSpan span) {
    }

    @Override
    public boolean isEndRequired() {
        return Boolean.FALSE;
    }

}
