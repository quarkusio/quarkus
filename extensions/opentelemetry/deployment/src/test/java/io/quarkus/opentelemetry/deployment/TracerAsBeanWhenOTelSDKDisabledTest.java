package io.quarkus.opentelemetry.deployment;

import static io.restassured.RestAssured.when;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.test.QuarkusUnitTest;

public class TracerAsBeanWhenOTelSDKDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class))
            .overrideRuntimeConfigKey("quarkus.otel.sdk.disabled", "true");

    @Test
    public void test() {
        when().get("resource")
                .then()
                .statusCode(200);
    }

    @Path("resource")
    public static class Resource {

        private final Tracer tracer;

        public Resource(Tracer tracer) {
            this.tracer = tracer;
        }

        @GET
        public String get() {
            Span span = tracer.spanBuilder("dummy").startSpan();
            try (Scope scope = span.makeCurrent()) {
                var otel = CDI.current().select(OpenTelemetry.class).get();
                return otel.toString();// we don't care about the string itself, all we care about is that OTel is initialized
            } catch (Throwable t) {
                span.recordException(t);
                throw t;
            } finally {
                span.end();
            }
        }
    }
}
