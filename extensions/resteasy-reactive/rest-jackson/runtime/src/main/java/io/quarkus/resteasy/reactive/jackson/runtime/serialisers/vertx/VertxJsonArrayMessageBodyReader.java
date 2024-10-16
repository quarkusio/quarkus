package io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx;

import java.io.IOException;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.vertx.core.json.JsonArray;

public class VertxJsonArrayMessageBodyReader extends VertxJsonArrayBasicMessageBodyReader
        implements ServerMessageBodyReader<JsonArray> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
        return isReadable(type);
    }

    @Override
    public JsonArray readFrom(Class<JsonArray> type, Type genericType, MediaType mediaType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        return readFrom(context.getInputStream());
    }
}
