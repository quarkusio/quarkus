package io.quarkus.it.opentelemetry.reactive;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import java.net.URI;
import java.time.Duration;
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
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.runtime.Startup;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;

@QuarkusTestResource(restrictToAnnotatedClass = true, value = OpenTelemetryWithSpanAtStartupTest.MyWireMockResource.class)
@QuarkusTest
public class OpenTelemetryWithSpanAtStartupTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    private static final int WIREMOCK_PORT = 20001;
    private static final String STARTUP_BEAN_ENABLED_PROPERTY = "startup.bean.enabled";

    @Test
    void testGeneratedSpansUsingRestClientReactive() {

        // There's a bit of asynchronousness in recording and exporting the spans, so to avoid issues, give the startup spans a (small) moment to arrive
        SpanDataList spans = spanExporter().awaitCount(2, Duration.ofSeconds(10));

        // First span is the callWireMockClient method. It does not have a parent span.
        SpanData client = spans.ofKind(INTERNAL).roots().single();
        assertThat(client).hasName("StartupBean.callWireMockClient");

        // We should get one client span, from the internal method.
        SpanData server = spans.ofKind(CLIENT).childrenOf(client).single();
        assertThat(server).hasName("GET /stub");
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
