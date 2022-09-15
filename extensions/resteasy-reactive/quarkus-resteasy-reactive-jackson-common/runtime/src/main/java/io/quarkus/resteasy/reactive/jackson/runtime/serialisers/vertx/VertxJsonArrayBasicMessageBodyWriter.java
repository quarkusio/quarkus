package io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;

import io.vertx.core.json.JsonArray;

public class VertxJsonArrayBasicMessageBodyWriter implements MessageBodyWriter<JsonArray> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isWriteable(type);
    }

    @Override
    public void writeTo(JsonArray o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(o.encode().getBytes(StandardCharsets.UTF_8));
    }

    protected boolean isWriteable(Class<?> type) {
        return JsonArray.class.isAssignableFrom(type);
    }
}
