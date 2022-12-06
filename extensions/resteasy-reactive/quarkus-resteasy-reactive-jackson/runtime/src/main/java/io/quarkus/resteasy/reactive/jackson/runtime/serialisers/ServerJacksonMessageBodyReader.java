package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.common.util.StreamUtil;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class ServerJacksonMessageBodyReader extends JacksonBasicMessageBodyReader implements ServerMessageBodyReader<Object> {

    @Inject
    public ServerJacksonMessageBodyReader(ObjectMapper mapper) {
        super(mapper);
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

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isReadable(mediaType, type);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
        return isReadable(mediaType, type);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, MediaType mediaType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        return doReadFrom(type, genericType, context.getInputStream());
    }

    private Object doReadFrom(Class<Object> type, Type genericType, InputStream entityStream) throws IOException {
        if (StreamUtil.isEmpty(entityStream)) {
            return null;
        }
        try {
            ObjectReader reader = getEffectiveReader();
            return reader.forType(reader.getTypeFactory().constructType(genericType != null ? genericType : type))
                    .readValue(entityStream);
        } catch (MismatchedInputException e) {
            if (isEmptyInputException(e)) {
                return null;
            }
            throw e;
        }
    }

    private boolean isEmptyInputException(MismatchedInputException e) {
        // this isn't great, but Jackson doesn't have a specific exception for empty input...
        return e.getMessage().startsWith("No content");
    }
}
