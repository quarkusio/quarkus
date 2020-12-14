package io.quarkus.resteasy.reactive.jsonb.runtime.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.common.util.EmptyInputStream;
import org.jboss.resteasy.reactive.server.providers.serialisers.json.AbstractJsonMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class JsonbMessageBodyReader extends AbstractJsonMessageBodyReader {

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

    private Object doReadFrom(Class<Object> type, Type genericType, InputStream entityStream) {
        if (entityStream instanceof EmptyInputStream) {
            return null;
        }
        return json.fromJson(entityStream, genericType != null ? genericType : type);
    }
}
