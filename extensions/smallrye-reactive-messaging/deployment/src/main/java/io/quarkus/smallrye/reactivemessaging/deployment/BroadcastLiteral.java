package io.quarkus.smallrye.reactivemessaging.deployment;

import java.lang.annotation.Annotation;

import io.smallrye.reactive.messaging.annotations.Broadcast;

public class BroadcastLiteral implements Broadcast {
    private final int subscribers;

    public BroadcastLiteral(int subscribers) {
        this.subscribers = subscribers;
    }

    @Override
    public int value() {
        return subscribers;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Broadcast.class;
    }
}
