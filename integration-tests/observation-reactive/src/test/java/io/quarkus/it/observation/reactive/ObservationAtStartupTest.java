package io.quarkus.it.observation.reactive;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
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

import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.SpanKind;
import io.quarkus.runtime.Startup;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;

@QuarkusTestResource(restrictToAnnotatedClass = true, value = ObservationAtStartupTest.MyWireMockResource.class)
@QuarkusTest
public class ObservationAtStartupTest {

    private static final int WIREMOCK_PORT = 20001;
    private static final String STARTUP_BEAN_ENABLED_PROPERTY = "startup.bean.enabled";

    @Test
    void observedMethodAtStartupProducesSpans() {
        // Wait for startup spans to arrive
        await().atMost(5, SECONDS).untilAsserted(() -> {
            List<Map<String, Object>> spans = getSpans();
            assertThat(spans).hasSizeGreaterThanOrEqualTo(2);
        });

        List<Map<String, Object>> spans = getSpans();

        // First span is the @Observed callWireMockClient method (INTERNAL, no parent)
        Map<String, Object> observedSpan = getSpanByKindAndParentId(spans, INTERNAL, "0000000000000000");
        assertThat(observedSpan).isNotNull();

        // Second span is the REST client call to WireMock (CLIENT, child of observed)
        Map<String, Object> clientSpan = getSpanByKindAndParentId(spans, CLIENT, observedSpan.get("spanId"));
        assertThat(clientSpan).isNotNull();
        assertThat(clientSpan.get("name")).isEqualTo("GET /stub");
        assertThat(clientSpan.get("traceId")).isEqualTo(observedSpan.get("traceId"));
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getSpans() {
        return RestAssured.get("/export").body().as(new TypeRef<>() {
        });
    }

    private Map<String, Object> getSpanByKindAndParentId(List<Map<String, Object>> spans,
            SpanKind kind, Object parentSpanId) {
        return spans.stream()
                .filter(s -> kind.toString().equals(s.get("kind")))
                .filter(s -> parentSpanId.equals(s.get("parentSpanId")))
                .findFirst()
                .orElse(null);
    }
}
