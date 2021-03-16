package io.quarkus.reactivemessaging.http.runtime.converters;

import java.lang.reflect.Type;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.MessageConverter;
import io.vertx.core.buffer.Buffer;

/**
 * Converts message payload to String
 */
@ApplicationScoped
public class StringConverter implements MessageConverter {

    @Override
    public boolean canConvert(Message<?> in, Type target) {
        return in.getPayload() instanceof Buffer && target == String.class;
    }

    @Override
    public Message<String> convert(Message<?> in, Type target) {
        return in.withPayload(((Buffer) in.getPayload()).toString());
    }
}
