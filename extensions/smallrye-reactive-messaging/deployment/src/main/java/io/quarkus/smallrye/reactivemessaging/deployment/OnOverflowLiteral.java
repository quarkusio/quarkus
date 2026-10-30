package io.quarkus.smallrye.reactivemessaging.deployment;

import java.lang.annotation.Annotation;

import jakarta.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.reactive.messaging.OnOverflow;

public class OnOverflowLiteral extends AnnotationLiteral<OnOverflow> implements OnOverflow {

    private static final long serialVersionUID = 1L;

    private final Strategy strategy;
    private final long buffer;

    OnOverflowLiteral(String strategy, long buffer) {
        this.strategy = strategy == null ? Strategy.BUFFER : Strategy.valueOf(strategy.toUpperCase());
        this.buffer = buffer;
    }

    public static OnOverflow create(String strategy, long buffer) {
        return new OnOverflowLiteral(strategy, buffer);
    }

    @Override
    public Strategy value() {
        return strategy;
    }

    @Override
    public long bufferSize() {
        return buffer;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return OnOverflow.class;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof OnOverflow that) {
            return this.strategy.equals(that.value()) && this.buffer == that.bufferSize();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = 0;
        result += 127 * "value".hashCode() ^ this.strategy.hashCode();
        result += 127 * "bufferSize".hashCode() ^ Long.hashCode(this.buffer);
        return result;
    }
}
