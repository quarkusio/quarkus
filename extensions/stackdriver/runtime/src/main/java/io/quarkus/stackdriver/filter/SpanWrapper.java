package io.quarkus.stackdriver.filter;

import java.util.concurrent.atomic.AtomicBoolean;

import io.opencensus.common.Scope;
import io.opencensus.trace.Span;

/**
 * Wrapper class used for exchanging span between filters.
 *
 * @author Pavol Loffay
 */
public class SpanWrapper {

    public static final String PROPERTY_NAME = SpanWrapper.class.getName() + ".activeSpanWrapper";

    private Scope scope;
    private Span span;
    private AtomicBoolean finished = new AtomicBoolean();

    public SpanWrapper(Scope scope, Span span) {
        this.scope = scope;
        this.span = span;
    }

    public Span get() {
        return span;
    }

    public Scope getScope() {
        return scope;
    }

    public synchronized void finish() {
        if (!finished.get()) {
            finished.set(true);
            span.end();
        }
    }

    public boolean isFinished() {
        return finished.get();
    }
}
