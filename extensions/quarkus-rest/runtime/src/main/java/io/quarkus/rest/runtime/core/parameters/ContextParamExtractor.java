package io.quarkus.rest.runtime.core.parameters;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestAsyncResponse;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResourceContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestSse;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestSseEventSink;
import io.quarkus.rest.runtime.spi.QuarkusRestContext;
import io.quarkus.rest.runtime.spi.SimplifiedResourceInfo;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class ContextParamExtractor implements ParameterExtractor {

    private final String type;

    public ContextParamExtractor(String type) {
        this.type = type;
    }

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        // NOTE: Same list for CDI at ContextProducers and in EndpointIndexer.CONTEXT_TYPES
        if (type.equals(QuarkusRestContext.class.getName())) {
            return context;
        }
        if (type.equals(HttpHeaders.class.getName())) {
            return context.getHttpHeaders();
        }
        if (type.equals(UriInfo.class.getName())) {
            return context.getUriInfo();
        }
        if (type.equals(Configuration.class.getName())) {
            return context.getDeployment().getConfiguration();
        }
        if (type.equals(AsyncResponse.class.getName())) {
            QuarkusRestAsyncResponse response = new QuarkusRestAsyncResponse(context);
            context.setAsyncResponse(response);
            return response;
        }
        if (type.equals(SseEventSink.class.getName())) {
            QuarkusRestSseEventSink sink = new QuarkusRestSseEventSink(context);
            context.setSseEventSink(sink);
            return sink;
        }
        if (type.equals(Request.class.getName())) {
            return context.getRequest();
        }
        if (type.equals(HttpServerResponse.class.getName())) {
            return context.getContext().response();
        }
        if (type.equals(HttpServerRequest.class.getName())) {
            return context.getContext().request();
        }
        if (type.equals(Providers.class.getName())) {
            return context.getProviders();
        }
        if (type.equals(Sse.class.getName())) {
            return QuarkusRestSse.INSTANCE;
        }
        if (type.equals(ResourceInfo.class.getName())) {
            return context.getTarget().getLazyMethod();
        }
        if (type.equals(SimplifiedResourceInfo.class.getName())) {
            return context.getTarget().getSimplifiedResourceInfo();
        }
        if (type.equals(Application.class.getName())) {
            return CDI.current().select(Application.class).get();
        }
        if (type.equals(SecurityContext.class.getName())) {
            return context.getSecurityContext();
        }
        if (type.equals(ResourceContext.class.getName())) {
            return QuarkusRestResourceContext.INSTANCE;
        }
        // FIXME: move to build time
        throw new IllegalStateException("Unsupported contextual type: " + type);
    }

}
