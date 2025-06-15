package io.quarkus.opentelemetry.deployment.propagation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.context.ThreadContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Span;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenTelemetryMpContextPropagationTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(OpenTelemetryMpContextPropagationTest.TestResource.class));

    @Test
    void testOpenTelemetryContextPropagationWithCustomExecutorAndThreadContextProvider() {
        String message = RestAssured.when().get("/helloWithContextPropagation").then().statusCode(200).extract()
                .asString();
        assertTrue(message.startsWith("Hello/"));
        String[] traceIds = message.split("/")[1].split("-");
        assertEquals(2, traceIds.length);
        assertEquals(traceIds[0], traceIds[1]);
    }

    @ApplicationScoped
    @Path("/")
    public static class TestResource {

        private final ExecutorService customExecutorService;

        private final ThreadContext threadContext;

        @Inject
        TestResource(ThreadContext threadContext) {
            this.customExecutorService = Executors.newWorkStealingPool();
            this.threadContext = threadContext;
        }

        @GET
        @Path("/helloWithContextPropagation")
        public CompletionStage<String> helloWithCustomExecutor() {
            String message = "Hello/" + Span.current().getSpanContext().getTraceId();
            return this.threadContext
                    .withContextCapture(CompletableFuture.supplyAsync(() -> message, this.customExecutorService))
                    .thenApplyAsync(msg -> msg + "-" + Span.current().getSpanContext().getTraceId(),
                            customExecutorService);
        }
    }
}
