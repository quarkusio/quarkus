package io.quarkus.rest.server.runtime.providers.serialisers.jsonp;

import java.io.ByteArrayOutputStream;

import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.runtime.providers.serialisers.jsonp.JsonStructureHandler;
import org.jboss.resteasy.reactive.common.runtime.providers.serialisers.jsonp.JsonpUtil;

import io.quarkus.rest.server.runtime.core.LazyMethod;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyWriter;
import io.vertx.core.buffer.Buffer;

public class ServerJsonStructureHandler extends JsonStructureHandler implements QuarkusRestMessageBodyWriter<JsonStructure> {

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return JsonStructure.class.isAssignableFrom(type) && !JsonObject.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(JsonStructure o, QuarkusRestRequestContext context) throws WebApplicationException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonWriter writer = JsonpUtil.writer(out, context.getResponseContentMediaType())) {
            writer.write(o);
        }
        context.getHttpServerResponse().end(Buffer.buffer(out.toByteArray()));
    }

}
