package org.jboss.resteasy.reactive.server.jsonb;

import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.common.providers.serialisers.AbstractJsonMessageBodyReader;
import org.jboss.resteasy.reactive.common.util.StreamUtil;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class JsonbMessageBodyReader extends AbstractJsonMessageBodyReader implements ServerMessageBodyReader<Object> {

    private final Jsonb json;

    @Inject
    public JsonbMessageBodyReader(Jsonb json) {
        this.json = json;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return doReadFrom(type, genericType, entityStream);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, MediaType mediaType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        return doReadFrom(type, genericType, context.getInputStream());
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isReadable(mediaType, type);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
        return isReadable(mediaType, type);
    }

    private Object doReadFrom(Class<Object> type, Type genericType, InputStream entityStream) {
        if (StreamUtil.isEmpty(entityStream)) {
            return null;
        }
        try {
            return json.fromJson(entityStream, genericType != null ? genericType : type);
        } catch (JsonbException e) {
            if (isEmptyInputException(e)) {
                return null;
            }
            throw e;
        }
    }

    private boolean isEmptyInputException(Exception e) {
        // this isn't great, but Yasson doesn't have a specific exception for empty input...
        return e.getMessage().contains("Invalid token=EOF at (line no=1, column no=0");
    }
}
