package io.quarkus.it.opentelemetry.reactive;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpanByKindAndParentId;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpans;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.runtime.Startup;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTestResource(restrictToAnnotatedClass = true, value = OpenTelemetryWithSpanAtStartupTest.MyWireMockResource.class)
@QuarkusTest
public class OpenTelemetryWithSpanAtStartupTest {

    private static final int WIREMOCK_PORT = 20001;
    private static final String STARTUP_BEAN_ENABLED_PROPERTY = "startup.bean.enabled";

    @Test
    void testGeneratedSpansUsingRestClientReactive() {

        // There's a bit of asynchronousness in recording and exporting the spans, so to avoid issues, give the startup spans a (small) moment to arrive
        await().atMost(Duration.ofSeconds(5L)).pollDelay(Duration.ofMillis(50)).until(() -> {
            // make sure incoming spans are processed
            List<Map<String, Object>> spans = getSpans();
            return spans.size() >= 1;
        });

        List<Map<String, Object>> spans = getSpans();
        assertEquals(2, spans.size());

        // First span is the callWireMockClient method. It does not have a parent span.
        Map<String, Object> client = getSpanByKindAndParentId(spans, INTERNAL, "0000000000000000");
        assertEquals("StartupBean.callWireMockClient", client.get("name"));

        // We should get one client span, from the internal method.
        Map<String, Object> server = getSpanByKindAndParentId(spans, CLIENT, client.get("spanId"));
        assertEquals("GET /stub", server.get("name"));
    }

    @Startup
    @ApplicationScoped
    public static class StartupBean {

        @ConfigProperty(name = STARTUP_BEAN_ENABLED_PROPERTY, defaultValue = "false")
        boolean enabled;

        @PostConstruct
        void onStart() {
            if (enabled) {
                callWireMockClient();
            }
        }

        @WithSpan
        public void callWireMockClient() {
            RestClientBuilder.newBuilder()
                    .baseUri(URI.create("http://localhost:" + WIREMOCK_PORT))
                    .build(WireMockRestClient.class)
                    .call();
        }
    }

    @Path("/stub")
    public interface WireMockRestClient {

        @GET
        void call();
    }

    public static class MyWireMockResource implements QuarkusTestResourceLifecycleManager {

        WireMockServer wireMockServer;

        @Override
        public Map<String, String> start() {
            wireMockServer = new WireMockServer(WIREMOCK_PORT);
            wireMockServer.stubFor(
                    WireMock.get(WireMock.urlMatching("/stub"))
                            .willReturn(ok()));
            wireMockServer.start();

            return Map.of(STARTUP_BEAN_ENABLED_PROPERTY, Boolean.TRUE.toString());
        }

        @Override
        public synchronized void stop() {
            if (wireMockServer != null) {
                wireMockServer.stop();
                wireMockServer = null;
            }
        }
    }
}
