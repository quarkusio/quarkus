package org.jboss.resteasy.reactive.client.spi;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;

public interface ClientMessageBodyWriter<T> extends MessageBodyWriter<T> {

    void writeTo(T t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream,
            RestClientRequestContext context)
            throws java.io.IOException, jakarta.ws.rs.WebApplicationException;

}
