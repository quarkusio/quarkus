package io.quarkus.resteasy.runtime.vertx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NoContentException;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;

/**
 * A body reader that allows to get a Vert.x {@link JsonArray} as JAX-RS request content.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JsonArrayReader implements MessageBodyReader<JsonArray> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == JsonArray.class;
    }

    @Override
    public JsonArray readFrom(Class<JsonArray> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        byte[] bytes = getBytes(entityStream);
        if (bytes.length == 0) {
            throw new NoContentException("Cannot create JsonArray");
        }
        return new JsonArray(Buffer.buffer(bytes));
    }

    private static byte[] getBytes(InputStream entityStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = entityStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }
}
