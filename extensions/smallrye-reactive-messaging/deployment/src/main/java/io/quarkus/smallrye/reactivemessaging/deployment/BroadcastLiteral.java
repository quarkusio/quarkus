package io.quarkus.smallrye.reactivemessaging.deployment;

import java.lang.annotation.Annotation;

import jakarta.enterprise.util.AnnotationLiteral;

import io.smallrye.reactive.messaging.annotations.Broadcast;

public class BroadcastLiteral extends AnnotationLiteral<Broadcast> implements Broadcast {

    private static final long serialVersionUID = 1L;

    private final int value;

    public BroadcastLiteral(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Broadcast.class;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof Broadcast that) {
            return this.value == that.value();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 127 * "value".hashCode() ^ Integer.hashCode(this.value);
    }
}
