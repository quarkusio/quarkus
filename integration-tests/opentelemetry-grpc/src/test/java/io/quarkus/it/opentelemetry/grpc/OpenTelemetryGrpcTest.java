package io.quarkus.it.opentelemetry.grpc;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_PEER_IP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_PEER_PORT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_TRANSPORT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.RPC_GRPC_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.RPC_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.RPC_SERVICE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.RPC_SYSTEM;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.opentelemetry.api.trace.SpanKind;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.vertx.core.http.HttpMethod;

@QuarkusTest
public class OpenTelemetryGrpcTest {
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

        Map<String, Object> server = spans.get(0);
        Map<String, Object> client = spans.get(1);
        Map<String, Object> rest = spans.get(2);

        assertEquals(server.get("traceId"), client.get("traceId"));
        assertEquals(server.get("traceId"), rest.get("traceId"));

        assertEquals("helloworld.Greeter/SayHello", server.get("name"));
        assertEquals(SpanKind.SERVER.toString(), server.get("kind"));
        assertEquals("grpc", getAttributes(server).get(RPC_SYSTEM.getKey()));
        assertEquals("helloworld.Greeter", getAttributes(server).get(RPC_SERVICE.getKey()));
        assertEquals("SayHello", getAttributes(server).get(RPC_METHOD.getKey()));
        assertEquals(Status.Code.OK.value(), getAttributes(server).get(RPC_GRPC_STATUS_CODE.getKey()));
        assertNotNull(getAttributes(server).get(NET_PEER_IP.getKey()));
        assertNotNull(getAttributes(server).get(NET_PEER_PORT.getKey()));
        assertEquals("ip_tcp", getAttributes(server).get(NET_TRANSPORT.getKey()));
        assertEquals(server.get("parentSpanId"), client.get("spanId"));

        assertEquals("helloworld.Greeter/SayHello", client.get("name"));
        assertEquals(SpanKind.CLIENT.toString(), client.get("kind"));
        assertEquals("grpc", getAttributes(client).get(RPC_SYSTEM.getKey()));
        assertEquals("helloworld.Greeter", getAttributes(client).get(RPC_SERVICE.getKey()));
        assertEquals("SayHello", getAttributes(client).get(RPC_METHOD.getKey()));
        assertEquals(Status.Code.OK.value(), getAttributes(client).get(RPC_GRPC_STATUS_CODE.getKey()));
        assertEquals(client.get("parentSpanId"), rest.get("spanId"));

        assertEquals(SpanKind.SERVER.toString(), rest.get("kind"));
        assertEquals("/grpc/{name}", rest.get("name"));
        assertEquals("/grpc/{name}", getAttributes(rest).get(HTTP_ROUTE.getKey()));
        assertEquals("/grpc/Naruto", getAttributes(rest).get(HTTP_TARGET.getKey()));
        assertEquals(HTTP_OK, getAttributes(rest).get(HTTP_STATUS_CODE.getKey()));
        assertEquals(HttpMethod.GET.name(), getAttributes(rest).get(HTTP_METHOD.getKey()));
    }

    private static List<Map<String, Object>> getSpans() {
        return when().get("/export").body().as(new TypeRef<>() {
        });
    }

    private static Map<?, ?> getAttributes(Map<String, Object> span) {
        return (Map<?, ?>) span.get("attributes");
    }
}
