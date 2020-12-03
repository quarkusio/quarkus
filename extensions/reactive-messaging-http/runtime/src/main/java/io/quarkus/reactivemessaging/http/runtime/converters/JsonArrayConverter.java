package io.quarkus.reactivemessaging.http.runtime.converters;

import java.lang.reflect.Type;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;

/**
 * Converts message payload to {@link JsonArray}
 */
@ApplicationScoped
public class JsonArrayConverter extends JacksonBasedConverter {

    @Override
    public boolean canConvert(Message<?> in, Type target) {
        return in.getPayload() instanceof Buffer && target == JsonArray.class;
    }

    @Override
    protected Message<JsonArray> doConvert(Message<?> in, Type target) {
        return in.withPayload(new JsonArray((Buffer) in.getPayload()));
    }
}
