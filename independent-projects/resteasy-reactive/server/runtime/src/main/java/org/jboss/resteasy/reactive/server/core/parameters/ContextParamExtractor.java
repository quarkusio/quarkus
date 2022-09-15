package org.jboss.resteasy.reactive.server.core.parameters;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.AsyncResponseImpl;
import org.jboss.resteasy.reactive.server.jaxrs.ResourceContextImpl;
import org.jboss.resteasy.reactive.server.jaxrs.SseEventSinkImpl;
import org.jboss.resteasy.reactive.server.jaxrs.SseImpl;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class ContextParamExtractor implements ParameterExtractor {

    private final Class<?> type;
    private volatile Instance<?> select;

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
        if (select == null) {
            select = CDI.current().select(type);
        }
        if (select != null) {
            instance = select.get();
        }
        if (instance != null) {
            return instance;
        }
        // FIXME: move to build time
        throw new IllegalStateException("Unsupported contextual type: " + type);
    }

}
