package io.quarkus.it.opentelemetry.reactive;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UrlAttributes.URL_QUERY;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.time.Duration;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;

@QuarkusTest
public class OpenTelemetryReactiveClientTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    @Inject
    @RestClient
    ReactiveRestClient client;

    @BeforeEach
    @AfterEach
    void reset() {
        spanExporter().reset();
    }

    @Test
    void get() {
        Uni<String> result = client.helloGet("Naruto");
        assertEquals("Hello Naruto", result.await().indefinitely());

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        // First span is the client call. It does not have a parent span.
        SpanData client = spans.ofKind(CLIENT).roots().single();
        assertThat(client)
                .hasName("GET /reactive")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, (long) HTTP_OK)
                .hasAttribute(HTTP_REQUEST_METHOD, HttpMethod.GET.name());

        // We should get one server span, from the client call. The client is the parent.
        SpanData server = spans.ofKind(SERVER).childrenOf(client).single();
        assertThat(server)
                .hasName("GET /reactive")
                .hasAttribute(HTTP_ROUTE, "/reactive")
                .hasAttribute(URL_PATH, "/reactive")
                .hasAttribute(URL_QUERY, "name=Naruto")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, (long) HTTP_OK)
                .hasAttribute(HTTP_REQUEST_METHOD, HttpMethod.GET.name());

        // Final span is an internal one, created by the resource method call. The server is the parent
        SpanData internal = spans.ofKind(INTERNAL).childrenOf(server).single();
        assertThat(internal).hasName("helloGet");
    }

    @Test
    void post() {
        Uni<String> result = client.helloPost("Naruto");
        assertEquals("Hello Naruto", result.await().indefinitely());

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        // First span is the client call. It does not have a parent span.
        SpanData client = spans.ofKind(CLIENT).roots().single();
        assertThat(client)
                .hasName("POST /reactive")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, (long) HTTP_OK)
                .hasAttribute(HTTP_REQUEST_METHOD, HttpMethod.POST.name());

        // We should get one server span, from the client call. The client is the parent.
        SpanData server = spans.ofKind(SERVER).childrenOf(client).single();
        assertThat(server)
                .hasName("POST /reactive")
                .hasAttribute(HTTP_ROUTE, "/reactive")
                .hasAttribute(URL_PATH, "/reactive")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, (long) HTTP_OK)
                .hasAttribute(HTTP_REQUEST_METHOD, HttpMethod.POST.name());

        // Final span is an internal one, created by the resource method call. The server is the parent
        SpanData internal = spans.ofKind(INTERNAL).childrenOf(server).single();
        assertThat(internal).hasName("helloPost");
    }
}
