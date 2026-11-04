package io.quarkus.resteasy.reactive.jackson.runtime.serialisers.vertx;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.resteasy.reactive.jackson.common.runtime.serialisers.vertx.VertxJsonArrayBasicMessageBodyReader;
import io.vertx.core.json.DecodeException;
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

    @Override
    protected JsonArray readFrom(InputStream entityStream) throws IOException {
        try {
            return super.readFrom(entityStream);
        } catch (DecodeException e) {
            // malformed JSON or a violated StreamReadConstraints limit is a client error
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
    }
}
