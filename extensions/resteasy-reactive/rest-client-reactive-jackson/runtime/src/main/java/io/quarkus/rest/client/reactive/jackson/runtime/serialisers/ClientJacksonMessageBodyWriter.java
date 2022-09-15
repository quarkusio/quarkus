package io.quarkus.rest.client.reactive.jackson.runtime.serialisers;

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
import jakarta.ws.rs.ext.MessageBodyWriter;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class ClientJacksonMessageBodyWriter implements MessageBodyWriter<Object>, ClientRestHandler {

    protected final ObjectMapper originalMapper;
    protected final ObjectWriter defaultWriter;
    private final ConcurrentMap<ObjectMapper, ObjectWriter> contextResolverMap = new ConcurrentHashMap<>();
    private RestClientRequestContext context;

    @Inject
    public ClientJacksonMessageBodyWriter(ObjectMapper mapper) {
        this.originalMapper = mapper;
        this.defaultWriter = createDefaultWriter(mapper);
    }

    @Override
    public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        doLegacyWrite(o, annotations, httpHeaders, entityStream, getEffectiveWriter());
    }

    @Override
    public void handle(RestClientRequestContext requestContext) throws Exception {
        this.context = context;
    }

    protected ObjectWriter getEffectiveWriter() {
        if (context == null) {
            // no context injected when writer is not running within a rest client context
            return defaultWriter;
        }

        ObjectMapper objectMapper = context.getConfiguration().getFromContext(ObjectMapper.class);
        if (objectMapper == null) {
            return defaultWriter;
        }

        return contextResolverMap.computeIfAbsent(objectMapper, new Function<>() {
            @Override
            public ObjectWriter apply(ObjectMapper objectMapper) {
                return createDefaultWriter(objectMapper);
            }
        });
    }
}
