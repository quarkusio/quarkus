package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget
import javax.ws.rs.sse.SseEventSource

@QuarkusTest
class FlowResourceTest {

    @TestHTTPResource("/flow")
    var flowPath: String? = null

    @Test
    fun testSeeStrings() {
        testSse("str", 5) {
            assertThat(it).containsExactly("Hello", "From", "Kotlin", "Flow")
        }
    }

    @Test
    fun testSeeJson() {
        testSse("json", 10) {
            assertThat(it).containsExactly(
                    "{\"name\":\"Barbados\",\"capital\":\"Bridgetown\"}",
                    "{\"name\":\"Mauritius\",\"capital\":\"Port Louis\"}",
                    "{\"name\":\"Fiji\",\"capital\":\"Suva\"}")
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
