package io.quarkus.opentelemetry.deployment.common;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.quarkus.test.QuarkusUnitTest;

@Path("/hello")
public class HelloResource {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.devservices.enabled", "false");

    @GET
    public String get() {
        // The span name is not updated with the route yet.
        return ((ReadableSpan) Span.current()).getName();
    }
}
