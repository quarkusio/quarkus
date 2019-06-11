package io.quarkus.vertx.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import io.vertx.core.json.JsonArray;

/**
 * A body writer that allows to return a Vert.x {@link JsonArray} as JAX-RS response content.
 *
 * @author Thomas Segismont
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JsonArrayWriter implements MessageBodyWriter<JsonArray> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == JsonArray.class;
    }

    @Override
    public void writeTo(JsonArray jsonArray, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(jsonArray.toBuffer().getBytes());
        entityStream.flush();
        entityStream.close();
    }
}
