package io.quarkus.it.resteasy.reactive.kotlin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.sse.SseEventSource
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@QuarkusTest
class FlowResourceTest {

    @TestHTTPResource("/flow") var flowPath: String? = null

    @Test
    fun testSseStrings() {
        testSse("str", 5) { assertThat(it).containsExactly("HELLO", "FROM", "KOTLIN", "FLOW") }
    }

    @Test
    fun testSuspendSseStrings() {
        testSse("suspendStr", 5) {
            assertThat(it).containsExactly("HELLO", "FROM", "KOTLIN", "FLOW")
        }
    }

    @Test
    fun testResponseStatusAndHeaders() {
        When { get("/flow/str") } Then
            {
                statusCode(201)
                headers(mapOf("foo" to "bar"))
            }
    }

    @Test
    fun testSeeJson() {

        testSse("json", 10) {
            assertThat(it.stream().map(this::json).collect(Collectors.toList()))
                .containsExactly(
                    json("{\"name\":\"Barbados\",\"capital\":\"Bridgetown\"}"),
                    json("{\"name\":\"Mauritius\",\"capital\":\"Port Louis\"}"),
                    json("{\"name\":\"Fiji\",\"capital\":\"Suva\"}"),
                )
        }
    }

    private val mapper: ObjectMapper = ObjectMapper()

    private fun json(jsonString: String?): JsonNode {
        try {
            return mapper.readTree(jsonString)
        } catch (e: java.lang.Exception) {
            throw java.lang.RuntimeException(e)
        }
    }

    private fun testSse(path: String, timeout: Long, assertion: (List<String>) -> Unit) {
        val client = ClientBuilder.newBuilder().build()
        val target: WebTarget = client.target("$flowPath/$path")
        SseEventSource.target(target)
            .reconnectingEvery(Int.MAX_VALUE.toLong(), TimeUnit.SECONDS)
            .build()
            .use { eventSource ->
                val res = CompletableFuture<List<String>>()
                val collect = Collections.synchronizedList(ArrayList<String>())
                eventSource.register(
                    { inboundSseEvent -> collect.add(inboundSseEvent.readData()) },
                    { throwable -> res.completeExceptionally(throwable) },
                ) {
                    res.complete(collect)
                }
                eventSource.open()
                val list = res.get(timeout, TimeUnit.SECONDS)
                assertion(list)
            }
    }
}
