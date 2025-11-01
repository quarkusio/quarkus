package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.StreamingOutput;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class StreamingOutputMessageBodyWriter implements ServerMessageBodyWriter<StreamingOutput> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return doIsWriteable(type);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target,
            MediaType mediaType) {
        return doIsWriteable(type);
    }

    private static boolean doIsWriteable(Class<?> type) {
        return StreamingOutput.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(StreamingOutput streamingOutput, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(StreamingOutput streamingOutput, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        streamingOutput.write(entityStream);
    }

    @Override
    public void writeResponse(StreamingOutput o, Type genericType, ServerRequestContext context)
            throws WebApplicationException {
        ResteasyReactiveRequestContext rrContext = (ResteasyReactiveRequestContext) context;
        try {
            o.write(context.getOrCreateOutputStream());
        } catch (Throwable t) {
            if (context.serverResponse().headWritten()) {
                context.serverResponse().reset();
                rrContext.serverRequest().closeConnection();
                rrContext.suspend();
            } else {
                if (t instanceof WebApplicationException) {
                    throw (WebApplicationException) t;
                } else if (t instanceof IOException) {
                    throw new WebApplicationException(t);
                } else if (t instanceof RuntimeException) {
                    throw new WebApplicationException(t);
                } else {
                    throw new WebApplicationException(t);
                }
            }
        }
    }
}
