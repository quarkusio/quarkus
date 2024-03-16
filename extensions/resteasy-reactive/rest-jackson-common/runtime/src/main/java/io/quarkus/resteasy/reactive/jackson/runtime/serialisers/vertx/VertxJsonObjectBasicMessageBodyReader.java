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

import io.vertx.core.json.JsonObject;

public class VertxJsonObjectBasicMessageBodyReader extends PrimitiveBodyHandler implements MessageBodyReader<JsonObject> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isReadable(type);
    }

    protected boolean isReadable(Class<?> type) {
        return JsonObject.class.isAssignableFrom(type);
    }

    @Override
    public JsonObject readFrom(Class<JsonObject> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return readFrom(entityStream);
    }

    protected JsonObject readFrom(InputStream entityStream) throws IOException {
        return new JsonObject(readFrom(entityStream, false));
    }
}
