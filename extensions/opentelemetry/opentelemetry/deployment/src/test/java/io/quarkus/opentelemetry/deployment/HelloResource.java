package io.quarkus.opentelemetry.deployment;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.ReadableSpan;

@Path("/hello")
public class HelloResource {

    @GET
    public String get() {
        return ((ReadableSpan) Span.current()).getName();
    }
}
