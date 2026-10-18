package io.quarkus.it.opentelemetry.vertx;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(classNames = { "io.opentelemetry.sdk.trace.data.SpanData" }, registerFullHierarchy = true)
public class SpanData {
}