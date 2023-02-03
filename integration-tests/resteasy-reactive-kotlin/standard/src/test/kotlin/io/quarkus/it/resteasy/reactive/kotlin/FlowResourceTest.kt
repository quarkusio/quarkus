package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.sse.SseEventSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@QuarkusTest
class FlowResourceTest {

    @TestHTTPResource("/flow")
    var flowPath: String? = null

    @Test
    fun testSseStrings() {
        testSse("str", 5) {
            assertThat(it).containsExactly("HELLO", "FROM", "KOTLIN", "FLOW")
        }
    }

    @Test
    fun testSuspendSseStrings() {
        testSse("suspendStr", 5) {
            assertThat(it).containsExactly("HELLO", "FROM", "KOTLIN", "FLOW")
        }
    }

    @Test
    fun testResponseStatusAndHeaders() {
        When {
            get("/flow/str")
        } Then {
            statusCode(201)
            headers(mapOf("foo" to "bar"))
        }
    }

    @Test
    fun testSeeJson() {
        testSse("json", 10) {
            assertThat(it).containsExactly(
                "{\"name\":\"Barbados\",\"capital\":\"Bridgetown\"}",
                "{\"name\":\"Mauritius\",\"capital\":\"Port Louis\"}",
                "{\"name\":\"Fiji\",\"capital\":\"Suva\"}"
            )
        }
    }

    private fun testSse(path: String, timeout: Long, assertion: (List<String>) -> Unit) {
        val client = ClientBuilder.newBuilder().build()
        val target: WebTarget = client.target("$flowPath/$path")
        SseEventSource.target(target).reconnectingEvery(Int.MAX_VALUE.toLong(), TimeUnit.SECONDS)
            .build().use { eventSource ->
                val res = CompletableFuture<List<String>>()
                val collect = Collections.synchronizedList(ArrayList<String>())
                eventSource.register({ inboundSseEvent -> collect.add(inboundSseEvent.readData()) }, { throwable -> res.completeExceptionally(throwable) }) { res.complete(collect) }
                eventSource.open()
                val list = res.get(timeout, TimeUnit.SECONDS)
                assertion(list)
            }
    }
}
