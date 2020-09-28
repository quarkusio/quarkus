package io.quarkus.rest.runtime.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import io.vertx.core.buffer.Buffer;

public class QuarkusRestClientWriterInterceptorContext extends QuarkusRestAbstractClientInterceptorContext
        implements WriterInterceptorContext {

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream(); //TODO: real bloocking IO
    boolean done = false;
    private int index = 0;
    private OutputStream outputStream = baos;

    private MultivaluedMap<String, String> headers;
    private Object entity;
    private MessageBodyWriter writer;
    private WriterInterceptor[] interceptors;

    private Buffer result;

    public QuarkusRestClientWriterInterceptorContext(WriterInterceptor[] writerInterceptors, MessageBodyWriter writer,
            Annotation[] annotations, Class<?> entityClass, Type entityType, Object entity,
            MediaType mediaType, MultivaluedMap<String, String> headers, Map<String, Object> properties) {
        super(annotations, entityClass, entityType, mediaType, properties);
        this.interceptors = writerInterceptors;
        this.writer = writer;
        this.entity = entity;
        this.headers = headers;
    }

    @Override
    public void proceed() throws IOException, WebApplicationException {
        if (index == interceptors.length) {
            writer.writeTo(entity, entityClass, entityType,
                    annotations, mediaType, headers, outputStream);
            outputStream.close();
            result = Buffer.buffer(baos.toByteArray());
            done = true;
        } else {
            interceptors[index++].aroundWriteTo(this);
        }
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public void setEntity(Object entity) {
        // FIXME: invalidate entityclass/type?
        this.entity = entity;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public void setOutputStream(OutputStream os) {
        this.outputStream = os;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return (MultivaluedMap) headers;
    }

    public Buffer getResult() {
        return result;
    }

}
