package io.quarkus.smallrye.opentracing.runtime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import io.opentracing.Span;
import io.opentracing.contrib.jaxrs2.internal.SpanWrapper;
import io.opentracing.tag.Tags;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Provider
public class QuarkusSmallRyeTracingStandaloneVertxDynamicFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        context.register(StandaloneFilter.class);
    }

    public static class StandaloneFilter implements ContainerRequestFilter {

        volatile CurrentVertxRequest currentVertxRequest;

        CurrentVertxRequest request() {
            if (currentVertxRequest == null) {
                currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
            }
            return currentVertxRequest;
        }

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            RoutingContext routingContext = request().getCurrent();
            routingContext.addHeadersEndHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    SpanWrapper wrapper = routingContext.get(SpanWrapper.PROPERTY_NAME);
                    if (wrapper != null) {
                        wrapper.getScope().close();
                        Tags.HTTP_STATUS.set(wrapper.get(), routingContext.response().getStatusCode());
                        if (routingContext.failure() != null) {
                            addExceptionLogs(wrapper.get(), routingContext.failure());
                        }
                        wrapper.finish();
                    }
                }
            });
        }

        private static void addExceptionLogs(Span span, Throwable throwable) {
            Tags.ERROR.set(span, true);
            if (throwable != null) {
                Map<String, Object> errorLogs = new HashMap<>(2);
                errorLogs.put("event", Tags.ERROR.getKey());
                errorLogs.put("error.object", throwable);
                span.log(errorLogs);
            }
        }
    }
}
