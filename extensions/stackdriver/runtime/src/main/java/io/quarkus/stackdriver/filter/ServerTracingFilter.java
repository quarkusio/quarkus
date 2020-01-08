package io.quarkus.stackdriver.filter;

import static io.quarkus.stackdriver.filter.SpanWrapper.PROPERTY_NAME;

import java.lang.annotation.Annotation;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.exception.ExceptionUtils;

import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.EndSpanOptions;
import io.opencensus.trace.Span;
import io.opencensus.trace.Status;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opentracing.tag.Tags;
import io.quarkus.stackdriver.runtime.SpanService;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;

public class ServerTracingFilter implements ContainerRequestFilter {
    private static final Logger log = Logger.getLogger(ServerTracingFilter.class.getName());

    private Pattern skipPattern;

    @Context
    private HttpServletRequest httpServletRequest;
    @Context
    private UriInfo uriInfo;
    volatile CurrentVertxRequest currentVertxRequest;
    private SpanService spanService;

    public ServerTracingFilter(SpanService spanService) {
        this.spanService = spanService;
        this.skipPattern = null;
    }

    private CurrentVertxRequest request() {
        if (this.currentVertxRequest == null) {
            this.currentVertxRequest = CDI.current().select(CurrentVertxRequest.class, new Annotation[0]).get();
        }

        return this.currentVertxRequest;
    }

    public void filter(ContainerRequestContext requestContext) {
        // return in case filter if registered twice
        Request request = requestContext.getRequest();
        if (requestContext.getProperty(PROPERTY_NAME) != null || matchesSkipPattern(uriInfo.getPath())) {
            return;
        }

        Tracer tracer = Tracing.getTracer();
        String requestSpanID = requestContext.getHeaderString("X-SpanID");
        String requestTraceID = requestContext.getHeaderString("X-TraceID");
        Scope scope = spanService.createSpan(requestSpanID, requestTraceID, request.getMethod(), uriInfo.getAbsolutePath());
        requestContext.setProperty(PROPERTY_NAME, new SpanWrapper(scope, tracer.getCurrentSpan()));
        RoutingContext routingContext = request().getCurrent();
        routingContext.addHeadersEndHandler(event -> {
            endRequestHandler(routingContext);
        });

    }

    private boolean matchesSkipPattern(String path) {
        // skip URLs matching skip pattern
        // e.g. pattern is defined as '/health|/status' then URL 'http://localhost:5000/context/health' won't be traced
        if (skipPattern != null && path != null) {
            if (path.length() > 0 && path.charAt(0) != '/') {
                path = "/" + path;
            }
            return skipPattern.matcher(path).matches();
        }
        return false;
    }

    private void endRequestHandler(RoutingContext routingContext) {
        SpanWrapper wrapper = routingContext.get(SpanWrapper.PROPERTY_NAME);
        if (wrapper != null) {
            if (routingContext.failure() != null) {
                wrapper.get().putAttribute(Tags.HTTP_STATUS.getKey(), AttributeValue.stringAttributeValue(String.valueOf(500)));
                addExceptionLogs(wrapper.get(), routingContext.failure());
            } else {
                wrapper.get().putAttribute(Tags.HTTP_STATUS.getKey(),
                        AttributeValue.stringAttributeValue(String.valueOf(routingContext.response().getStatusCode())));
                wrapper.get().end(EndSpanOptions.builder().setStatus(Status.OK).build());
            }
            wrapper.finish();
            wrapper.getScope().close();
            routingContext.put(SpanWrapper.PROPERTY_NAME, null);
        } else {
            routingContext.next();
        }
    }

    private void addExceptionLogs(Span span, Throwable throwable) {
        if (throwable != null) {
            span.putAttribute("trace", AttributeValue.stringAttributeValue(ExceptionUtils.getStackTrace(throwable)));
            span.putAttribute("error.object", AttributeValue.stringAttributeValue(ExceptionUtils.getStackTrace(throwable)));
            span.setStatus(Status.INTERNAL);
            span.end(EndSpanOptions.builder().setStatus(Status.INTERNAL.withDescription(throwable.getMessage())).build());
        }

    }

}
