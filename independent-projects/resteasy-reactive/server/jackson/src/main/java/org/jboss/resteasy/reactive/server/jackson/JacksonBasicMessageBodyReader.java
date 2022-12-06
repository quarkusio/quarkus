package org.jboss.resteasy.reactive.server.jackson;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.common.providers.serialisers.AbstractJsonMessageBodyReader;
import org.jboss.resteasy.reactive.common.util.EmptyInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class JacksonBasicMessageBodyReader extends AbstractJsonMessageBodyReader {

    protected final ObjectReader defaultReader;

    @Inject
    public JacksonBasicMessageBodyReader(ObjectMapper mapper) {
        this.defaultReader = mapper.reader();
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            return doReadFrom(type, genericType, entityStream);
        } catch (MismatchedInputException e) {
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
    }

    protected ObjectReader getEffectiveReader() {
        return defaultReader;
    }

    private Object doReadFrom(Class<Object> type, Type genericType, InputStream entityStream) throws IOException {
        if (entityStream instanceof EmptyInputStream) {
            return null;
        }
        ObjectReader reader = getEffectiveReader();
        return reader.forType(reader.getTypeFactory().constructType(genericType != null ? genericType : type))
                .readValue(entityStream);
    }
}
