package org.jboss.resteasy.reactive.server.providers.serialisers.jsonp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonStructureHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonpUtil;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class ServerJsonStructureHandler extends JsonStructureHandler
        implements ServerMessageBodyWriter<JsonStructure>, ServerMessageBodyReader<JsonStructure> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return JsonStructure.class.isAssignableFrom(type) && !JsonObject.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(JsonStructure o, Type genericType, ServerRequestContext context) throws WebApplicationException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonWriter writer = JsonpUtil.writer(out, context.getResponseMediaType())) {
            writer.write(o);
        }
        context.serverResponse().end(out.toByteArray());
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod,
            MediaType mediaType) {
        return JsonStructure.class.isAssignableFrom(type) && !JsonObject.class.isAssignableFrom(type);
    }

    @Override
    public JsonStructure readFrom(Class<JsonStructure> type, Type genericType, MediaType mediaType,
            ServerRequestContext context) throws WebApplicationException, IOException {
        return JsonpUtil.reader(context.getInputStream(), mediaType).read();
    }
}
