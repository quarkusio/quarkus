package io.quarkus.rest.client.reactive.jackson.runtime.serialisers;

import static io.quarkus.rest.client.reactive.jackson.runtime.serialisers.JacksonUtil.getObjectMapperFromContext;
import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.createDefaultWriter;
import static org.jboss.resteasy.reactive.server.jackson.JacksonMessageBodyWriterUtil.doLegacyWrite;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientMessageBodyWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class ClientJacksonMessageBodyWriter implements ClientMessageBodyWriter<Object> {

    private final ObjectWriter defaultWriter;
    private final ConcurrentMap<ObjectMapper, ObjectWriter> objectWriterMap = new ConcurrentHashMap<>();

    @Inject
    public ClientJacksonMessageBodyWriter(ObjectMapper mapper) {
        this.defaultWriter = createDefaultWriter(mapper);
    }

    @Override
    public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        doLegacyWrite(o, annotations, httpHeaders, entityStream, getEffectiveWriter(mediaType, null));
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream,
            RestClientRequestContext context) throws IOException, WebApplicationException {
        doLegacyWrite(o, annotations, httpHeaders, entityStream, getEffectiveWriter(mediaType, context));
    }

    protected ObjectWriter getEffectiveWriter(MediaType responseMediaType, RestClientRequestContext context) {
        ObjectMapper objectMapper = getObjectMapperFromContext(responseMediaType, context);
        if (objectMapper == null) {
            return defaultWriter;
        }

        return objectWriterMap.computeIfAbsent(objectMapper, new Function<>() {
            @Override
            public ObjectWriter apply(ObjectMapper objectMapper) {
                return createDefaultWriter(objectMapper);
            }
        });
    }
}
