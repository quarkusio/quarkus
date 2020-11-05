package io.quarkus.rest.server.runtime.providers.serialisers.jsonp;

import java.io.ByteArrayOutputStream;

import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.runtime.providers.serialisers.jsonp.JsonObjectHandler;
import org.jboss.resteasy.reactive.common.runtime.providers.serialisers.jsonp.JsonpUtil;

import io.quarkus.rest.server.runtime.core.LazyMethod;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyWriter;
import io.vertx.core.buffer.Buffer;

public class ServerJsonObjectHandler extends JsonObjectHandler implements QuarkusRestMessageBodyWriter<JsonObject> {

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return JsonObject.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(JsonObject o, QuarkusRestRequestContext context) throws WebApplicationException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonWriter writer = JsonpUtil.writer(out, context.getResponseContentMediaType())) {
            writer.writeObject(o);
        }
        context.getHttpServerResponse().end(Buffer.buffer(out.toByteArray()));
    }

}
