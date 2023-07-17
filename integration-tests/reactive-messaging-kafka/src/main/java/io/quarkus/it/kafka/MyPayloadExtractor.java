package io.quarkus.it.kafka;

import java.lang.reflect.Type;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.keyed.KeyValueExtractor;

@ApplicationScoped
public class MyPayloadExtractor implements KeyValueExtractor {
    @Override
    public boolean canExtract(Message<?> message, Type type, Type type1) {
        return false; // Only used with @Keyed
    }

    @Override
    public Object extractKey(Message<?> message, Type type) {
        String[] seg = ((String) message.getPayload()).split("-");
        return seg[0].toUpperCase();
    }

    @Override
    public Object extractValue(Message<?> message, Type type) {
        String[] seg = ((String) message.getPayload()).split("-");
        return seg[1];
    }
}
