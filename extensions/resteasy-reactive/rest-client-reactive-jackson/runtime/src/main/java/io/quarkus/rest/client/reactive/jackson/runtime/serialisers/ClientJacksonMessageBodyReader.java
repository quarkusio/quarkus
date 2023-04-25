package io.quarkus.rest.client.reactive.jackson.runtime.serialisers;

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
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class ClientJacksonMessageBodyReader extends JacksonBasicMessageBodyReader implements ClientRestHandler {

    private static final Logger log = Logger.getLogger(ClientJacksonMessageBodyReader.class);

    private final ConcurrentMap<ObjectMapper, ObjectReader> contextResolverMap = new ConcurrentHashMap<>();
    private RestClientRequestContext context;

    @Inject
    public ClientJacksonMessageBodyReader(ObjectMapper mapper) {
        super(mapper);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            return super.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
        } catch (JsonParseException e) {
            log.debug("Server returned invalid json data", e);
            throw new ClientWebApplicationException(e, Response.Status.OK);
        } catch (StreamReadException | DatabindException e) {
            throw new ClientWebApplicationException(e, Response.Status.BAD_REQUEST); // TODO: we need to check if this actually makes sense...
        }
    }

    @Override
    public void handle(RestClientRequestContext requestContext) {
        this.context = requestContext;
    }

    @Override
    protected ObjectReader getEffectiveReader() {
        if (context == null) {
            // no context injected when reader is not running within a rest client context
            return super.getEffectiveReader();
        }

        ObjectMapper objectMapper = context.getConfiguration().getFromContext(ObjectMapper.class);
        if (objectMapper == null) {
            return super.getEffectiveReader();
        }

        return contextResolverMap.computeIfAbsent(objectMapper, new Function<>() {
            @Override
            public ObjectReader apply(ObjectMapper objectMapper) {
                return objectMapper.reader();
            }
        });
    }
}
