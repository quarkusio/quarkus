package io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import io.vertx.core.json.JsonObject;

public class VertxJsonObjectBasicMessageBodyWriter implements MessageBodyWriter<JsonObject> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isWriteable(type);
    }

    protected boolean isWriteable(Class<?> type) {
        return JsonObject.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(JsonObject o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(o.encode().getBytes(StandardCharsets.UTF_8));
    }

}
