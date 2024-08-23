package org.jboss.resteasy.reactive.server.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;

public class WriterInterceptorContextImpl extends AbstractInterceptorContext
        implements WriterInterceptorContext {

    private final WriterInterceptor[] interceptors;
    private final MessageBodyWriter writer;
    private final MultivaluedMap<String, Object> headers = new CaseInsensitiveMap<>();
    private Object entity;

    boolean done = false;
    private int index = 0;

    public WriterInterceptorContextImpl(ResteasyReactiveRequestContext context, WriterInterceptor[] interceptors,
            MessageBodyWriter<?> writer, Annotation[] annotations, Class<?> type, Type genericType, Object entity,
            MediaType mediaType, MultivaluedMap<String, Object> headers, ServerSerialisers serialisers) {
        super(context, annotations, type, genericType, mediaType, serialisers);
        this.interceptors = interceptors;
        this.writer = writer;
        this.entity = entity;
        this.headers.putAll(headers);
    }

    @Override
    public void proceed() throws IOException, WebApplicationException {
        Response response = context.getResponse().get();
        // this is needed in order to avoid having the headers written out twice
        context.serverResponse().setPreCommitListener(null);
        if (index == interceptors.length) {
            MessageBodyWriter effectiveWriter = writer;
            if (rediscoveryNeeded) {
                List<MessageBodyWriter<?>> newWriters = serialisers.findWriters(null, entity.getClass(), mediaType,
                        RuntimeType.SERVER);
                if (newWriters.isEmpty()) {
                    throw new InternalServerErrorException("Could not find MessageBodyWriter for " + entity.getClass(),
                            Response.serverError().build());
                }
                effectiveWriter = newWriters.get(0);
            }
            context.setResult(Response.fromResponse(response).replaceAll(headers).build());
            ServerSerialisers.encodeResponseHeaders(context);
            // this must be done AFTER encoding the headers, otherwise the HTTP response gets all messed up
            effectiveWriter.writeTo(entity, type, genericType,
                    annotations, mediaType, response.getHeaders(), context.getOrCreateOutputStream());
            context.getOutputStream().close();
            done = true;
        } else {
            interceptors[index++].aroundWriteTo(this);
            if (!done) {
                //TODO: how to handle
                context.setResult(Response.fromResponse(response).replaceAll(headers).build());
                ServerSerialisers.encodeResponseHeaders(context);
                context.serverResponse().end();
            }
        }

    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public void setEntity(Object entity) {
        this.entity = entity;
    }

    @Override
    public OutputStream getOutputStream() {
        OutputStream existing = context.getOutputStream();
        if (existing != null) {
            return existing;
        }
        return context.getOrCreateOutputStream();
    }

    @Override
    public void setOutputStream(OutputStream os) {
        context.setOutputStream(os);
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }
}
