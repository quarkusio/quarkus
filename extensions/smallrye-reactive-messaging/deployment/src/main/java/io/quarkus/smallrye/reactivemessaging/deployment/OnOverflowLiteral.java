package io.quarkus.smallrye.reactivemessaging.deployment;

import java.lang.annotation.Annotation;

import org.eclipse.microprofile.reactive.messaging.OnOverflow;

public class OnOverflowLiteral implements OnOverflow {

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
}
