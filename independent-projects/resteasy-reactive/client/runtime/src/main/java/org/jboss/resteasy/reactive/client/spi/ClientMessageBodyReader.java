package org.jboss.resteasy.reactive.client.spi;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;

public interface ClientMessageBodyReader<T> extends MessageBodyReader<T> {

    T readFrom(Class<T> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream,
            RestClientRequestContext context) throws java.io.IOException, jakarta.ws.rs.WebApplicationException;
}
