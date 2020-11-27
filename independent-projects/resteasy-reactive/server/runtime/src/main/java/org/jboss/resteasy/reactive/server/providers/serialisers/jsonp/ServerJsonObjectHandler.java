package org.jboss.resteasy.reactive.server.providers.serialisers.jsonp;

import java.io.ByteArrayOutputStream;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonObjectHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonpUtil;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class ServerJsonObjectHandler extends JsonObjectHandler implements ServerMessageBodyWriter<JsonObject> {

    @Override
    public boolean isWriteable(Class<?> type, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return JsonObject.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(JsonObject o, ServerRequestContext context) throws WebApplicationException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonWriter writer = JsonpUtil.writer(out, context.getResponseMediaType())) {
            writer.writeObject(o);
        }
        context.serverResponse().end(out.toByteArray());
    }

}
