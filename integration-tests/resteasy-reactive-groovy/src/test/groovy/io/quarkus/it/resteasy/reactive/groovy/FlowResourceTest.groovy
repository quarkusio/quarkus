package io.quarkus.it.resteasy.reactive.groovy

import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.sse.SseEventSource
import org.junit.jupiter.api.Test

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

import static io.restassured.RestAssured.given
import static org.assertj.core.api.Assertions.assertThat

@QuarkusTest
class FlowResourceTest {

    @TestHTTPResource("/flow")
    String flowPath

    @Test
    void testSseStrings() {
        testSse("str", 5) { assertThat(it).containsExactly("HELLO", "FROM", "KOTLIN", "FLOW") }
    }

    @Test
    void testSuspendSseStrings() {
        testSse("suspendStr", 5) {
            assertThat(it).containsExactly("HELLO", "FROM", "KOTLIN", "FLOW")
        }
    }

    @Test
    void testResponseStatusAndHeaders() {
        given()
            .when()
            .get("/flow/str")
            .then()
            .statusCode(201)
            .headers(["foo": "bar"])
    }

    @Test
    void testSeeJson() {
        testSse("json", 10) {
            assertThat(it)
                .containsExactly(
                    "{\"name\":\"Barbados\",\"capital\":\"Bridgetown\"}",
                    "{\"name\":\"Mauritius\",\"capital\":\"Port Louis\"}",
                    "{\"name\":\"Fiji\",\"capital\":\"Suva\"}"
                )
        }
    }

    private testSse(String path, long timeout, Consumer<List<String>> assertion) {
        var client = ClientBuilder.newBuilder().build()
        WebTarget target = client.target("$flowPath/$path")
        try (SseEventSource eventSource = SseEventSource.target(target)
            .reconnectingEvery(Integer.MAX_VALUE.toLong(), TimeUnit.SECONDS)
            .build()) {
                var res = new CompletableFuture<List<String>>()
                var collect = Collections.synchronizedList(new ArrayList<String>())
                eventSource.register(
                    { inboundSseEvent -> collect.add(inboundSseEvent.readData()) },
                    { throwable -> res.completeExceptionally(throwable) }
                ) {
                    res.complete(collect)
                }
                eventSource.open()
                var list = res.get(timeout, TimeUnit.SECONDS)
                assertion(list)
            }
    }
}
