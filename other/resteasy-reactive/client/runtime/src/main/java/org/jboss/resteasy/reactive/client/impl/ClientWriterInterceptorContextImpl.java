package org.jboss.resteasy.reactive.client.impl;

import io.vertx.core.buffer.Buffer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;

public class ClientWriterInterceptorContextImpl extends AbstractClientInterceptorContextImpl
        implements WriterInterceptorContext {

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream(); //TODO: real bloocking IO
    boolean done = false;
    private int index = 0;
    private OutputStream outputStream = baos;
    private final Serialisers serialisers;
    private final ConfigurationImpl configuration;
    // as the interceptors can change the type or mediaType, when that happens we need to find a new reader/writer
    protected boolean rediscoveryNeeded = false;

    private MultivaluedMap<String, String> headers;
    private Object entity;
    private MessageBodyWriter writer;
    private WriterInterceptor[] interceptors;

    private Buffer result;

    public ClientWriterInterceptorContextImpl(WriterInterceptor[] writerInterceptors, MessageBodyWriter writer,
            Annotation[] annotations, Class<?> entityClass, Type entityType, Object entity,
            MediaType mediaType, MultivaluedMap<String, String> headers, Map<String, Object> properties,
            Serialisers serialisers, ConfigurationImpl configuration) {
        super(annotations, entityClass, entityType, mediaType, properties);
        this.interceptors = writerInterceptors;
        this.writer = writer;
        this.entity = entity;
        this.headers = headers;
        this.serialisers = serialisers;
        this.configuration = configuration;
    }

    @Override
    public void proceed() throws IOException, WebApplicationException {
        if (index == interceptors.length) {
            MessageBodyWriter effectiveWriter = writer;
            if (rediscoveryNeeded) {
                List<MessageBodyWriter<?>> newWriters = serialisers.findWriters(configuration, entityClass, mediaType,
                        RuntimeType.CLIENT);
                if (newWriters.isEmpty()) {
                    // FIXME: exception?
                    return;
                }
                effectiveWriter = newWriters.get(0);
            }
            effectiveWriter.writeTo(entity, entityClass, entityType,
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

    @Override
    public void setType(Class<?> type) {
        if ((this.entityClass != type) && (type != null)) {
            rediscoveryNeeded = true;
        }
        this.entityClass = type;
        // FIXME: invalidate generic type?
    }

    @Override
    public void setMediaType(MediaType mediaType) {
        if (this.mediaType != mediaType) {
            rediscoveryNeeded = true;
        }
        this.mediaType = mediaType;
    }

}
