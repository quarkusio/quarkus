package io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;

import org.jboss.resteasy.reactive.common.providers.serialisers.PrimitiveBodyHandler;

import io.vertx.core.json.JsonArray;

public class VertxJsonArrayBasicMessageBodyReader extends PrimitiveBodyHandler implements MessageBodyReader<JsonArray> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isReadable(type);
    }

    @Override
    public JsonArray readFrom(Class<JsonArray> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return readFrom(entityStream);
    }

    protected boolean isReadable(Class<?> type) {
        return JsonArray.class.isAssignableFrom(type);
    }

    protected JsonArray readFrom(InputStream entityStream) throws IOException {
        return new JsonArray(readFrom(entityStream, false));
    }
}
