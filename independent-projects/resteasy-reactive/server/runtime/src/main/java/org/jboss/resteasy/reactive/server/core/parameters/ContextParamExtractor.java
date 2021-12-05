package org.jboss.resteasy.reactive.server.core.parameters;

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
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.AsyncResponseImpl;
import org.jboss.resteasy.reactive.server.jaxrs.ResourceContextImpl;
import org.jboss.resteasy.reactive.server.jaxrs.SseEventSinkImpl;
import org.jboss.resteasy.reactive.server.jaxrs.SseImpl;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class ContextParamExtractor implements ParameterExtractor {

    private final Class<?> type;

    public ContextParamExtractor(String type) {
        try {
            this.type = Class.forName(type, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public ContextParamExtractor(Class<?> type) {
        this.type = type;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        // NOTE: Same list for CDI at ContextProducers and in EndpointIndexer.CONTEXT_TYPES
        if (type.equals(ServerRequestContext.class)) {
            return context;
        }
        if (type.equals(HttpHeaders.class)) {
            return context.getHttpHeaders();
        }
        if (type.equals(UriInfo.class)) {
            return context.getUriInfo();
        }
        if (type.equals(Configuration.class)) {
            return context.getDeployment().getConfiguration();
        }
        if (type.equals(AsyncResponse.class)) {
            AsyncResponseImpl response = new AsyncResponseImpl(context);
            context.setAsyncResponse(response);
            return response;
        }
        if (type.equals(SseEventSink.class)) {
            SseEventSinkImpl sink = new SseEventSinkImpl(context);
            context.setSseEventSink(sink);
            return sink;
        }
        if (type.equals(Request.class)) {
            return context.getRequest();
        }
        if (type.equals(Providers.class)) {
            return context.getProviders();
        }
        if (type.equals(Sse.class)) {
            return SseImpl.INSTANCE;
        }
        if (type.equals(ResourceInfo.class)) {
            return context.getTarget().getLazyMethod();
        }
        if (type.equals(SimpleResourceInfo.class)) {
            return context.getTarget().getSimplifiedResourceInfo();
        }
        if (type.equals(Application.class)) {
            return CDI.current().select(Application.class).get();
        }
        if (type.equals(SecurityContext.class)) {
            return context.getSecurityContext();
        }
        if (type.equals(ResourceContext.class)) {
            return ResourceContextImpl.INSTANCE;
        }
        Object instance = context.unwrap(type);
        if (instance != null) {
            return instance;
        }
        // FIXME: move to build time
        throw new IllegalStateException("Unsupported contextual type: " + type);
    }

}
