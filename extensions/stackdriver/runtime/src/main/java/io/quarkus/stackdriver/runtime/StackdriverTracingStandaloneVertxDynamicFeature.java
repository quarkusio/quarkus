//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package io.quarkus.stackdriver.runtime;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opentracing.tag.Tags;
import io.quarkus.arc.Arc;
import io.quarkus.stackdriver.filter.ServerTracingFilter;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Provider
@ApplicationScoped
public class StackdriverTracingStandaloneVertxDynamicFeature implements DynamicFeature {

    private final static Logger LOGGER = LoggerFactory.getLogger(StackdriverTracingStandaloneVertxDynamicFeature.class);

    private SpanService spanService;

    public StackdriverTracingStandaloneVertxDynamicFeature() {
        this.spanService = Arc.container().instance(SpanService.class).get();
    }

    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        context.register(new ServerTracingFilter(spanService));
    }

    void configureErrorHandling(@Observes Router router) {
        router.get().handler(this::noMatch);
    }

    private void noMatch(RoutingContext routingContext) {
        routingContext.addHeadersEndHandler(event -> {
            int statusCode = routingContext.response().getStatusCode();
            if (statusCode >= 400 && statusCode < 500) {
                HttpServerRequest request = routingContext.request();
                LOGGER.info("nomatch {}", request.absoluteURI());
                Tracer tracer = Tracing.getTracer();
                if (tracer != null) {
                    URI requestURI = URI.create(request.absoluteURI());
                    try (Scope scope = spanService.createSpan(request.rawMethod(), requestURI)) {
                        Span span = tracer.getCurrentSpan();
                        span.putAttribute(Tags.HTTP_STATUS.getKey(),
                                AttributeValue.stringAttributeValue(String.valueOf(statusCode)));
                    }
                }
            }
        });
        routingContext.next();
    }
}
