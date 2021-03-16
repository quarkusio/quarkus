package io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.vertx.core.json.JsonArray;

public class VertxJsonArrayMessageBodyWriter implements ServerMessageBodyWriter<JsonArray> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return isWriteable(type);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isWriteable(type);
    }

    private boolean isWriteable(Class<?> type) {
        return JsonArray.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(JsonArray o, Type genericType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        context.serverResponse().end(o.encode());
    }

    @Override
    public void writeTo(JsonArray o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(o.encode().getBytes(StandardCharsets.UTF_8));
    }
}
