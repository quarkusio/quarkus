package io.quarkus.opentelemetry.runtime.tracing.security;

import jakarta.enterprise.context.Dependent;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * Main purpose of this processor is to cover adding of the End User attributes to user-created Spans.
 */
@Dependent
public class EndUserSpanProcessor implements SpanProcessor {

    @Override
    public void onStart(Context context, ReadWriteSpan span) {
        SecurityEventUtil.addEndUserAttributes(span);
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan readableSpan) {

    }

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
