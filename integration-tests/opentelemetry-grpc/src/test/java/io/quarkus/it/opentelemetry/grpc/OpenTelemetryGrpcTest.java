package io.quarkus.it.opentelemetry.grpc;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_METHOD;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_SYSTEM_NAME;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.vertx.core.http.HttpMethod;

@QuarkusTest
public class OpenTelemetryGrpcTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    @BeforeEach
    @AfterEach
    void reset() {
        spanExporter().reset();
    }

    @Test
    void grpc() {
        given()
                .contentType("application/json")
                .when()
                .get("/grpc/{name}", "Naruto")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Naruto"));

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        // First span is the rest server call. It does not have a parent span.
        SpanData rest = spans.ofKind(SERVER).roots().single();
        assertThat(rest)
                .hasName("GET /grpc/{name}")
                .hasAttribute(HTTP_ROUTE, "/grpc/{name}")
                .hasAttribute(URL_PATH, "/grpc/Naruto")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, (long) HTTP_OK)
                .hasAttribute(HTTP_REQUEST_METHOD, HttpMethod.GET.name());

        // Second span is the gRPC client call
        SpanData client = spans.ofKind(CLIENT).childrenOf(rest).single();
        assertThat(client)
                .hasName("helloworld.Greeter/SayHello")
                .hasAttribute(RPC_SYSTEM_NAME, "grpc")
                .hasAttribute(RPC_METHOD, "helloworld.Greeter/SayHello")
                .hasAttribute(RPC_RESPONSE_STATUS_CODE, String.valueOf(Status.Code.OK.value()));

        // Third span is the gRPC server call
        SpanData server = spans.ofKind(SERVER).childrenOf(client).single();
        assertThat(server)
                .hasName("helloworld.Greeter/SayHello")
                .hasAttribute(RPC_SYSTEM_NAME, "grpc")
                .hasAttribute(RPC_METHOD, "helloworld.Greeter/SayHello")
                .hasAttribute(RPC_RESPONSE_STATUS_CODE, String.valueOf(Status.Code.OK.value()));
        assertNotNull(server.getAttributes().get(SERVER_ADDRESS));
        assertNotNull(server.getAttributes().get(SERVER_PORT));
    }
}
