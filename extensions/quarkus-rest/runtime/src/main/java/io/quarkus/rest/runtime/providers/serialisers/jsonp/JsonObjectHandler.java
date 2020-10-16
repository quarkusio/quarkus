package io.quarkus.rest.runtime.providers.serialisers.jsonp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.spi.QuarkusRestMessageBodyWriter;
import io.vertx.core.buffer.Buffer;

public class JsonObjectHandler implements MessageBodyReader<JsonObject>, QuarkusRestMessageBodyWriter<JsonObject> {

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return JsonObject.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(JsonObject o, QuarkusRestRequestContext context) throws WebApplicationException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonWriter writer = JsonpUtil.writer(out, context.getResponseContentMediaType())) {
            writer.writeObject(o);
        }
        context.getContext().response().end(Buffer.buffer(out.toByteArray()));
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonObject.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(JsonObject o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        try (JsonWriter writer = JsonpUtil.writer(entityStream, mediaType)) {
            writer.writeObject(o);
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonObject.class.isAssignableFrom(type);
    }

    @Override
    public JsonObject readFrom(Class<JsonObject> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return JsonpUtil.reader(entityStream, mediaType).readObject();
    }
}
