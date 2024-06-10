package io.quarkus.it.vertx.kotlin

import io.quarkus.vertx.ConsumeEvent
import jakarta.inject.Singleton
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.delay

@Singleton
class SuspendMessageConsumers {
    companion object {
        @Volatile var latch: CountDownLatch = CountDownLatch(1)

        var message: String? = null

        fun reset() {
            latch = CountDownLatch(1)
            message = null
        }
    }

    @ConsumeEvent("suspend-message")
    suspend fun message(msg: io.vertx.core.eventbus.Message<String>) {
        delay(100)
        message = msg.body()
        latch.countDown()
    }

    @ConsumeEvent("suspend-mutiny-message")
    suspend fun mutinyMessage(msg: io.vertx.mutiny.core.eventbus.Message<String>) {
        delay(100)
        message = msg.body()
        latch.countDown()
    }

    @ConsumeEvent("suspend-payload")
    suspend fun payload(msg: String) {
        delay(100)
        message = msg
        latch.countDown()
    }

    @ConsumeEvent("suspend-headers-payload")
    suspend fun headersPayload(headers: io.vertx.core.MultiMap, msg: String) {
        delay(100)
        message = "${headers["header"]} - $msg"
        latch.countDown()
    }

    @ConsumeEvent("suspend-mutiny-headers-payload")
    suspend fun mutinyHeadersPayload(headers: io.vertx.mutiny.core.MultiMap, msg: String) {
        delay(100)
        message = "${headers["header"]} - $msg"
        latch.countDown()
    }
}
