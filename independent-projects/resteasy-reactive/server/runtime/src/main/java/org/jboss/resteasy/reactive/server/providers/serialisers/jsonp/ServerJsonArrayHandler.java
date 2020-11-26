package org.jboss.resteasy.reactive.server.providers.serialisers.jsonp;

import java.io.ByteArrayOutputStream;
import javax.json.JsonArray;
import javax.json.JsonWriter;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonArrayHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonpUtil;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.LazyMethod;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveMessageBodyWriter;

public class ServerJsonArrayHandler extends JsonArrayHandler implements ResteasyReactiveMessageBodyWriter<JsonArray> {

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return JsonArray.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(JsonArray o, ResteasyReactiveRequestContext context) throws WebApplicationException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonWriter writer = JsonpUtil.writer(out, context.getResponseContentType().getMediaType())) {
            writer.writeArray(o);
        }
        context.serverResponse().end(out.toByteArray());
    }

}
