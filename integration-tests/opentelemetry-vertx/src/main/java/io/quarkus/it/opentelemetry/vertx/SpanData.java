package io.quarkus.it.opentelemetry.vertx;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(classNames = {
        "io.opentelemetry.sdk.trace.data.SpanData",
        "io.opentelemetry.sdk.trace.SpanWrapper",
        "io.opentelemetry.sdk.trace.AutoValue_SpanWrapper",
        "io.opentelemetry.sdk.trace.data.StatusData",
        "io.opentelemetry.sdk.trace.data.ImmutableStatusData",
        "io.opentelemetry.sdk.trace.data.AutoValue_ImmutableStatusData",
        "io.opentelemetry.api.trace.SpanContext",
        "io.opentelemetry.api.internal.ImmutableSpanContext",
        "io.opentelemetry.api.internal.AutoValue_ImmutableSpanContext",
        "io.opentelemetry.api.trace.TraceFlags",
        "io.opentelemetry.api.trace.ImmutableTraceFlags",
        "io.opentelemetry.api.trace.TraceState",
        "io.opentelemetry.api.trace.ArrayBasedTraceState",
        "io.opentelemetry.api.trace.AutoValue_ArrayBasedTraceState",
        "io.opentelemetry.sdk.common.InstrumentationLibraryInfo",
        "io.opentelemetry.sdk.common.AutoValue_InstrumentationLibraryInfo",
        "io.opentelemetry.sdk.resources.Resource",
        "io.opentelemetry.sdk.resources.AutoValue_Resource",
        "io.opentelemetry.api.common.Attributes",
        "io.quarkus.opentelemetry.runtime.tracing.DelayedAttributes"
})
public class SpanData {
}
