package io.quarkus.it.opentelemetry.vertx;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.vertx.core.http.HttpMethod;

@QuarkusTest
class HelloRouterTest {
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 0);
    }

    @Test
    void span() {
        given()
                .get("/hello")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("hello"));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 1);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(1, spans.size());

        assertEquals(SERVER.toString(), spans.get(0).get("kind"));
        assertEquals("hello", spans.get(0).get("name"));
        assertEquals(HTTP_OK, ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_STATUS_CODE.toString()));
        assertEquals(HttpMethod.GET.toString(), ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_METHOD.toString()));
        assertEquals("/hello", ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_ROUTE.toString()));
    }

    @Test
    void spanPath() {
        given()
                .get("/hello/{name}", "Naruto")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("hello Naruto"));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 1);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(1, spans.size());

        assertEquals(SERVER.toString(), spans.get(0).get("kind"));
        assertEquals("hello/:name", spans.get(0).get("name"));
        assertEquals(HTTP_OK, ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_STATUS_CODE.toString()));
        assertEquals(HttpMethod.GET.toString(), ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_METHOD.toString()));
        assertEquals("/hello/:name", ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_ROUTE.toString()));
    }

    private static List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }
}
