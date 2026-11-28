package io.quarkus.it.observation.reactive;

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

import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.runtime.Startup;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;

@QuarkusTestResource(restrictToAnnotatedClass = true, value = ObservationAtStartupTest.MyWireMockResource.class)
@QuarkusTest
public class ObservationAtStartupTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    private static final int WIREMOCK_PORT = 20001;
    private static final String STARTUP_BEAN_ENABLED_PROPERTY = "startup.bean.enabled";

    @Test
    void observedMethodAtStartupProducesSpans() {
        // Wait for startup spans to arrive
        SpanDataList spans = spanExporter().awaitUntil("at least 2 spans", Duration.ofSeconds(5), s -> s.size() >= 2);
        TracesAssert.assertThat(spans).hasSize(1);

        // First span is the @Observed callWireMockClient method (INTERNAL, no parent)
        SpanData observedSpan = spans.ofKind(INTERNAL).roots().single();

        // Second span is the REST client call to WireMock (CLIENT, child of observed)
        SpanData clientSpan = spans.ofKind(CLIENT).childrenOf(observedSpan).single();
        assertThat(clientSpan).hasName("GET /stub");
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

        @Observed
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
