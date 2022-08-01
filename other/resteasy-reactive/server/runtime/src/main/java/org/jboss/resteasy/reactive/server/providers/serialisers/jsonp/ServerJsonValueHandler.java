package org.jboss.resteasy.reactive.server.providers.serialisers.jsonp;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonValueHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonpUtil;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

public class ServerJsonValueHandler extends JsonValueHandler implements ServerMessageBodyWriter<JsonValue> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return JsonValue.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(JsonValue o, Type genericType, ServerRequestContext context) throws WebApplicationException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonWriter writer = JsonpUtil.writer(out, context.getResponseMediaType())) {
            writer.write(o);
        }
        context.serverResponse().end(out.toByteArray());
    }

}
