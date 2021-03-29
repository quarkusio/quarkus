package io.quarkus.reactivemessaging.http.runtime.converters;

import java.lang.reflect.Type;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.vertx.core.buffer.Buffer;

/**
 * Converts message payload to objects of specified class.
 *
 * Used as the last converter
 */
@ApplicationScoped
public class ObjectConverter extends JacksonBasedConverter {

    @Override
    public boolean canConvert(Message<?> message, Type type) {
        if (!(type instanceof Class)) {
            return false;
        }
        Class<?> theClass = (Class<?>) type;
        return message.getPayload() instanceof Buffer
                && !Buffer.class.isAssignableFrom(theClass)
                && Object.class.isAssignableFrom((Class<?>) type);
    }

    @Override
    protected Message<?> doConvert(Message<?> message, Type type) {
        Buffer buffer = (Buffer) message.getPayload();
        Object result = buffer.toJsonObject().mapTo((Class<?>) type);
        return message.withPayload(result);
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE; // use this converter only if there is no specific converter
    }
}
