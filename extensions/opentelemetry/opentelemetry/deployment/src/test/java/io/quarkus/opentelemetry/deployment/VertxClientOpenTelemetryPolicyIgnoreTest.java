package io.quarkus.opentelemetry.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class VertxClientOpenTelemetryPolicyIgnoreTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(TestSpanExporter.class));

    @Inject
    TestSpanExporter spanExporter;
    @Inject
    Vertx vertx;
    @TestHTTPResource
    URI uri;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void testPolicyIgnoreClient() throws Exception {
        WebClientOptions options = new WebClientOptions();
        options.setTracingPolicy(TracingPolicy.IGNORE);

        Future<HttpResponse<Buffer>> response = WebClient.create(vertx, options)
                .get(uri.getPort(), uri.getHost(), "/hello")
                .send();

        response.onComplete(event -> {
            assertTrue(event.succeeded());
            List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        });
    }

    @ApplicationScoped
    public static class HelloRouter {
        @Inject
        Router router;
        @Inject
        Vertx vertx;

        public void register(@Observes StartupEvent ev) {
            router.get("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
