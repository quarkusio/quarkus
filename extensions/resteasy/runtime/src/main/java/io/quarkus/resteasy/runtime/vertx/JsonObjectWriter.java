package io.quarkus.resteasy.runtime.vertx;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.AsyncMessageBodyWriter;
import org.jboss.resteasy.spi.AsyncOutputStream;

import io.vertx.core.json.JsonObject;

/**
 * A body writer that allows to return a Vert.x {@link JsonObject} as JAX-RS response content.
 *
 * @author Thomas Segismont
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JsonObjectWriter implements AsyncMessageBodyWriter<JsonObject> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == JsonObject.class;
    }

    @Override
    public void writeTo(JsonObject jsonObject, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(jsonObject.toBuffer().getBytes());
        entityStream.flush();
        entityStream.close();
    }

    @Override
    public CompletionStage<Void> asyncWriteTo(JsonObject jsonObject, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, AsyncOutputStream entityStream) {
        return entityStream.asyncWrite(jsonObject.toBuffer().getBytes());
    }
}
