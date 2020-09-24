package io.quarkus.rest.runtime.jaxrs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

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

public class QuarkusRestWriterInterceptorContext implements WriterInterceptorContext {

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream(); //TODO: real bloocking IO
    private final QuarkusRestRequestContext context;
    private final WriterInterceptor[] interceptors;
    private final MessageBodyWriter writer;
    private Annotation[] annotations;
    private Class<?> type;
    private Type genericType;
    private OutputStream outputStream = baos;
    private Object entity;
    private MediaType mediaType;
    private final MultivaluedMap<String, Object> headers;

    boolean done = false;
    private int index = 0;

    public QuarkusRestWriterInterceptorContext(QuarkusRestRequestContext context, WriterInterceptor[] interceptors,
            MessageBodyWriter<?> writer, Annotation[] annotations, Class<?> type, Type genericType, Object entity,
            MediaType mediaType, MultivaluedMap<String, Object> headers) {
        this.context = context;
        this.interceptors = interceptors;
        this.writer = writer;
        this.annotations = annotations;
        this.type = type;
        this.genericType = genericType;
        this.entity = entity;
        this.mediaType = mediaType;
        this.headers = new CaseInsensitiveMap<>();
        this.headers.putAll(headers);
    }

    @Override
    public void proceed() throws IOException, WebApplicationException {
        if (index == interceptors.length) {
            writer.writeTo(entity, type, genericType,
                    annotations, mediaType, context.getResponse().getHeaders(), outputStream);
            context.setResult(Response.fromResponse(context.getResponse()).replaceAll(headers).build());
            Serialisers.encodeResponseHeaders(context);
            outputStream.close();
            context.getContext().response().end(Buffer.buffer(baos.toByteArray()));
            done = true;
        } else {
            interceptors[index++].aroundWriteTo(this);
            if (done) {
                //TODO: how to handle
                context.setResult(Response.fromResponse(context.getResponse()).replaceAll(headers).build());
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
        return outputStream;
    }

    @Override
    public void setOutputStream(OutputStream os) {
        outputStream = os;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }

    @Override
    public Object getProperty(String name) {
        return context.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return context.getPropertyNames();
    }

    @Override
    public void setProperty(String name, Object object) {
        context.setProperty(name, object);
    }

    @Override
    public void removeProperty(String name) {
        context.removeProperty(name);
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations;
    }

    @Override
    public void setAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public void setType(Class<?> type) {
        this.type = type;
    }

    @Override
    public Type getGenericType() {
        return genericType;
    }

    @Override
    public void setGenericType(Type genericType) {
        this.genericType = genericType;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }
}
