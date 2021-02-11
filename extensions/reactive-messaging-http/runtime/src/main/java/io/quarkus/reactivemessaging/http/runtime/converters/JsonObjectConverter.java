package io.quarkus.reactivemessaging.http.runtime.converters;

import java.lang.reflect.Type;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

/**
 * Converts message payload to {@link JsonObject}
 */
@ApplicationScoped
public class JsonObjectConverter extends JacksonBasedConverter {

    @Override
    public boolean canConvert(Message<?> in, Type target) {
        return in.getPayload() instanceof Buffer && target == JsonObject.class;
    }

    @Override
    protected Message<JsonObject> doConvert(Message<?> in, Type target) {
        return in.withPayload(new JsonObject((Buffer) in.getPayload()));
    }
}
