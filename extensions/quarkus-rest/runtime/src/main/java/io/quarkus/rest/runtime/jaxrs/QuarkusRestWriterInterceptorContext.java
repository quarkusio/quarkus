package io.quarkus.rest.runtime.jaxrs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.util.CaseInsensitiveMap;
import io.vertx.core.buffer.Buffer;

public class QuarkusRestWriterInterceptorContext extends QuarkusRestAbstractInterceptorContext
        implements WriterInterceptorContext {

    private final WriterInterceptor[] interceptors;
    private final MessageBodyWriter writer;
    private final MultivaluedMap<String, Object> headers = new CaseInsensitiveMap<>();
    private Object entity;

    boolean done = false;
    private int index = 0;

    public QuarkusRestWriterInterceptorContext(QuarkusRestRequestContext context, WriterInterceptor[] interceptors,
            MessageBodyWriter<?> writer, Annotation[] annotations, Class<?> type, Type genericType, Object entity,
            MediaType mediaType, MultivaluedMap<String, Object> headers, Serialisers serialisers) {
        super(context, annotations, type, genericType, mediaType, serialisers);
        this.interceptors = interceptors;
        this.writer = writer;
        this.entity = entity;
        this.headers.putAll(headers);
    }

    @Override
    public void proceed() throws IOException, WebApplicationException {
        Response response = context.getResponse().get();
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
            ByteArrayOutputStream baos = context.getOrCreateOutputStream();
            effectiveWriter.writeTo(entity, type, genericType,
                    annotations, mediaType, response.getHeaders(), context.getOutputStream());
            context.setResult(Response.fromResponse(response).replaceAll(headers).build());
            Serialisers.encodeResponseHeaders(context);
            context.getOutputStream().close();
            context.getContext().response().end(Buffer.buffer(baos.toByteArray()));
            done = true;
        } else {
            interceptors[index++].aroundWriteTo(this);
            if (!done) {
                //TODO: how to handle
                context.setResult(Response.fromResponse(response).replaceAll(headers).build());
                Serialisers.encodeResponseHeaders(context);
                context.getContext().response().end();
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
