package io.quarkus.stackdriver.runtime;

import static io.opencensus.trace.Annotation.fromDescriptionAndAttributes;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opentracing.tag.Tags;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Dependent
public class StackdriverProducer {

    private final static Logger LOGGER = LoggerFactory.getLogger(StackdriverTracingStandaloneVertxDynamicFeature.class);

    private Tracer tracer;
    private SpanService spanService;

    public StackdriverProducer() {
        tracer = Tracing.getTracer();
        spanService = new SpanService(tracer());
    }

    @Produces
    public SpanService spanService() {
        return spanService;
    }

    @Produces
    public Tracer tracer() {
        return tracer;
    }

    void configureErrorHandling(@Observes Router router) {
        router.get().handler(this::noMatch);
    }

    private void noMatch(RoutingContext routingContext) {
        routingContext.addHeadersEndHandler(event -> {
            int statusCode = routingContext.response().getStatusCode();
            if (statusCode >= 400 && statusCode < 500) {
                HttpServerRequest request = routingContext.request();
                LOGGER.info("No matching route {}", request.absoluteURI());
                if (tracer != null) {
                    URI requestURI = URI.create(request.absoluteURI());
                    try (Scope scope = spanService.createSpan(request.rawMethod(), requestURI)) {
                        Span span = tracer.getCurrentSpan();
                        span.putAttribute(Tags.HTTP_STATUS.getKey(),
                                AttributeValue.stringAttributeValue(String.valueOf(statusCode)));
                        span.putAttribute("requested url",
                                AttributeValue.stringAttributeValue(request.absoluteURI()));
                    }
                }
            }
        });
        routingContext.next();
    }
}
