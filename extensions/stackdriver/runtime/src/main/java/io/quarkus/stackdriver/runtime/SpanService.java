package io.quarkus.stackdriver.runtime;

import static io.opencensus.trace.Annotation.fromDescriptionAndAttributes;

import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.interceptor.InvocationContext;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.EndSpanOptions;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.SpanId;
import io.opencensus.trace.Status;
import io.opencensus.trace.TraceId;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracestate;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import io.opentracing.tag.Tags;
import io.quarkus.stackdriver.Span;

@ApplicationScoped
public class SpanService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpanService.class);
    private final Tracer tracer;

    public SpanService() {
        this.tracer = Tracing.getTracer();
    }

    public Object computeTrace(InvocationContext invocationCtx) throws Exception {
        Scope scope = Tracing.getTracer()
                .spanBuilderWithExplicitParent(getSpanLabel(invocationCtx), Tracing.getTracer().getCurrentSpan())
                .startScopedSpan();
        io.opencensus.trace.Span currentSpan = Tracing.getTracer().getCurrentSpan();
        try {
            //execute the intercepted method and store the return value
            Object result = invocationCtx.proceed();
            currentSpan.setStatus(Status.OK);
            currentSpan.putAttribute(Tags.HTTP_STATUS.getKey(),
                    AttributeValue.stringAttributeValue(String.valueOf(HttpStatus.SC_OK)));
            currentSpan.end();
            return result;
        } catch (Exception e) {
            currentSpan.setStatus(Status.INTERNAL);
            currentSpan.putAttribute(Tags.HTTP_STATUS.getKey(),
                    AttributeValue.stringAttributeValue(String.valueOf(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
            currentSpan.putAttribute("trace", AttributeValue.stringAttributeValue(e.getMessage()));
            currentSpan.end(EndSpanOptions.builder().setStatus(Status.INTERNAL.withDescription(e.getMessage())).build());
            throw e;
        } finally {
            scope.close();
        }
    }

    private String getSpanLabel(InvocationContext invocationCtx) {
        String annotatedSpanLabel = invocationCtx.getMethod().getDeclaredAnnotation(Span.class).value();
        return !annotatedSpanLabel.isEmpty() ? annotatedSpanLabel
                : MessageFormat.format("{0}.{1}",
                        invocationCtx.getTarget().getClass().getSimpleName(),
                        invocationCtx.getMethod().getName());
    }

    public Scope createSpan(String spanName, URI requestURI) {
        return createSpan(null, null, spanName, requestURI);
    }

    public Scope createSpan(String requestSpanID, String requestTraceID, String spanName, URI requestURI) {
        Optional<SpanContext> remoteContext = buildRemoteContext(requestSpanID, requestTraceID);
        SpanBuilder spanBuilder = remoteContext.isPresent()
                ? tracer.spanBuilderWithRemoteParent(spanName, remoteContext.get())
                : tracer.spanBuilder(spanName);

        Scope scope = spanBuilder.setSampler(Samplers.alwaysSample()).startScopedSpan();
        io.opencensus.trace.Span span = tracer.getCurrentSpan();
        span.putAttribute(Tags.HTTP_METHOD.getKey(), AttributeValue.stringAttributeValue(spanName));
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put("component", AttributeValue.stringAttributeValue("quarkus"));
        attributes.put(Tags.PEER_HOSTNAME.getKey(), AttributeValue.stringAttributeValue(requestURI.getHost()));
        attributes.put(Tags.PEER_PORT.getKey(), AttributeValue.longAttributeValue(requestURI.getPort()));
        attributes.put(Tags.HTTP_URL.getKey(), AttributeValue.stringAttributeValue(requestURI.toString()));
        span.addAnnotation(fromDescriptionAndAttributes("Details", attributes));
        return scope;
    }

    private Optional<SpanContext> buildRemoteContext(String requestSpanID, String requestTraceID) {
        if (requestSpanID != null && requestTraceID != null) {
            return Optional.of(SpanContext.create(TraceId.fromLowerBase16(requestTraceID),
                    SpanId.fromLowerBase16(requestSpanID),
                    null,
                    Tracestate.builder().build()));
        }
        return Optional.empty();
    }
}
