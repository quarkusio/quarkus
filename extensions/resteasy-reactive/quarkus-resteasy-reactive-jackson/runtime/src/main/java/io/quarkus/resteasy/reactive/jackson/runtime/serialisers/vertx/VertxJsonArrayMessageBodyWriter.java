package io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx;

import java.io.IOException;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.vertx.core.json.JsonArray;

public class VertxJsonArrayMessageBodyWriter extends VertxJsonArrayBasicMessageBodyWriter
        implements ServerMessageBodyWriter<JsonArray> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return isWriteable(type);
    }

    @Override
    public void writeResponse(JsonArray o, Type genericType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        context.serverResponse().end(o.encode());
    }
}
