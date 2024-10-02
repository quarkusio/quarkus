package io.quarkus.resteasy.reactive.server.test.security.inheritance;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.vertx.core.json.JsonObject;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JsonObjectReader implements ServerMessageBodyReader<JsonObject> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
        return true;
    }

    @Override
    public JsonObject readFrom(Class<JsonObject> type, Type genericType, MediaType mediaType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        return readFrom(context.getInputStream());
    }

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public JsonObject readFrom(Class<JsonObject> aClass, Type type, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> multivaluedMap, InputStream inputStream)
            throws IOException, WebApplicationException {
        return readFrom(inputStream);
    }

    private JsonObject readFrom(InputStream inputStream) {
        try {
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return new JsonObject(json);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse JsonObject.", e);
        }
    }
}
