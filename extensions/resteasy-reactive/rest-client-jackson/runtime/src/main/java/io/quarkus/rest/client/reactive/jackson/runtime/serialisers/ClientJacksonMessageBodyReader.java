package io.quarkus.rest.client.reactive.jackson.runtime.serialisers;

import static io.quarkus.rest.client.reactive.jackson.runtime.serialisers.JacksonUtil.getObjectMapperFromContext;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientMessageBodyReader;
import org.jboss.resteasy.reactive.common.providers.serialisers.AbstractJsonMessageBodyReader;
import org.jboss.resteasy.reactive.common.util.EmptyInputStream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class ClientJacksonMessageBodyReader extends AbstractJsonMessageBodyReader implements ClientMessageBodyReader<Object> {

    private static final Logger log = Logger.getLogger(ClientJacksonMessageBodyReader.class);

    private final ConcurrentMap<ObjectMapper, ObjectReader> objectReaderMap = new ConcurrentHashMap<>();
    private final ObjectReader defaultReader;

    @Inject
    public ClientJacksonMessageBodyReader(ObjectMapper mapper) {
        this.defaultReader = mapper.reader();
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return doRead(type, genericType, mediaType, entityStream, null);
    }

    private Object doRead(Class<Object> type, Type genericType, MediaType mediaType, InputStream entityStream,
            RestClientRequestContext context)
            throws IOException {
        try {
            if (entityStream instanceof EmptyInputStream) {
                return null;
            }
            ObjectReader reader = getEffectiveReader(mediaType, context);
            return reader.forType(reader.getTypeFactory().constructType(genericType != null ? genericType : type))
                    .readValue(entityStream);

        } catch (JsonParseException e) {
            log.debug("Server returned invalid json data", e);
            throw new ClientWebApplicationException(e, Response.Status.OK);
        }
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream,
            RestClientRequestContext context) throws java.io.IOException, jakarta.ws.rs.WebApplicationException {
        return doRead(type, genericType, mediaType, entityStream, context);
    }

    private ObjectReader getEffectiveReader(MediaType responseMediaType, RestClientRequestContext context) {
        ObjectMapper effectiveMapper = getObjectMapperFromContext(responseMediaType, context);
        if (effectiveMapper == null) {
            return defaultReader;
        }

        return objectReaderMap.computeIfAbsent(effectiveMapper, new Function<>() {
            @Override
            public ObjectReader apply(ObjectMapper objectMapper) {
                return objectMapper.reader();
            }
        });
    }
}
