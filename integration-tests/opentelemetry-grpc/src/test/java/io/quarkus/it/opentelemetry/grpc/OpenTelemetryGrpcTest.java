package io.quarkus.it.opentelemetry.grpc;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.SemanticAttributes.NET_HOST_NAME;
import static io.opentelemetry.semconv.SemanticAttributes.NET_HOST_PORT;
import static io.opentelemetry.semconv.SemanticAttributes.RPC_GRPC_STATUS_CODE;
import static io.opentelemetry.semconv.SemanticAttributes.RPC_METHOD;
import static io.opentelemetry.semconv.SemanticAttributes.RPC_SERVICE;
import static io.opentelemetry.semconv.SemanticAttributes.RPC_SYSTEM;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.opentelemetry.api.trace.SpanKind;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.vertx.core.http.HttpMethod;

@QuarkusTest
public class OpenTelemetryGrpcTest {

    @BeforeEach
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 0);
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

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        // First span is the rest server call. It does not have a parent span.
        Map<String, Object> rest = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals(SpanKind.SERVER.toString(), rest.get("kind"));
        assertEquals("GET /grpc/{name}", rest.get("name"));
        assertEquals("/grpc/{name}", getAttributes(rest).get(HTTP_ROUTE.getKey()));
        assertEquals("/grpc/Naruto", getAttributes(rest).get(HTTP_TARGET.getKey()));
        assertEquals(HTTP_OK, getAttributes(rest).get(HTTP_STATUS_CODE.getKey()));
        assertEquals(HttpMethod.GET.name(), getAttributes(rest).get(HTTP_METHOD.getKey()));

        // Second span is the gRPC client call
        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, rest.get("spanId"));
        assertEquals("helloworld.Greeter/SayHello", client.get("name"));
        assertEquals(SpanKind.CLIENT.toString(), client.get("kind"));
        assertEquals("grpc", getAttributes(client).get(RPC_SYSTEM.getKey()));
        assertEquals("helloworld.Greeter", getAttributes(client).get(RPC_SERVICE.getKey()));
        assertEquals("SayHello", getAttributes(client).get(RPC_METHOD.getKey()));
        assertEquals(Status.Code.OK.value(), getAttributes(client).get(RPC_GRPC_STATUS_CODE.getKey()));
        assertEquals(client.get("parentSpanId"), rest.get("spanId"));

        // Third span is the gRPC server call
        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals("helloworld.Greeter/SayHello", server.get("name"));
        assertEquals(SpanKind.SERVER.toString(), server.get("kind"));
        assertEquals("grpc", getAttributes(server).get(RPC_SYSTEM.getKey()));
        assertEquals("helloworld.Greeter", getAttributes(server).get(RPC_SERVICE.getKey()));
        assertEquals("SayHello", getAttributes(server).get(RPC_METHOD.getKey()));
        assertEquals(Status.Code.OK.value(), getAttributes(server).get(RPC_GRPC_STATUS_CODE.getKey()));
        assertNotNull(getAttributes(server).get(NET_HOST_NAME.getKey()));
        assertNotNull(getAttributes(server).get(NET_HOST_PORT.getKey()));
        assertEquals(server.get("parentSpanId"), client.get("spanId"));
    }

    private static List<Map<String, Object>> getSpans() {
        return when().get("/export").body().as(new TypeRef<>() {
        });
    }

    private static Map<?, ?> getAttributes(Map<String, Object> span) {
        return (Map<?, ?>) span.get("attributes");
    }

    private static List<Map<String, Object>> getSpansByKindAndParentId(List<Map<String, Object>> spans, SpanKind kind,
            Object parentSpanId) {
        return spans.stream()
                .filter(map -> map.get("kind").equals(kind.toString()))
                .filter(map -> map.get("parentSpanId").equals(parentSpanId)).collect(toList());
    }

    private static Map<String, Object> getSpanByKindAndParentId(List<Map<String, Object>> spans, SpanKind kind,
            Object parentSpanId) {
        List<Map<String, Object>> span = getSpansByKindAndParentId(spans, kind, parentSpanId);
        assertEquals(1, span.size());
        return span.get(0);
    }
}
