package io.quarkus.it.vertx.kotlin

import io.quarkus.vertx.ConsumeEvent
import jakarta.inject.Singleton
import java.util.concurrent.CountDownLatch

@Singleton
class MessageConsumers {
    companion object {
        @Volatile var latch: CountDownLatch = CountDownLatch(1)

        var message: String? = null

        fun reset() {
            latch = CountDownLatch(1)
            message = null
        }
    }

    @ConsumeEvent("message")
    fun message(msg: io.vertx.core.eventbus.Message<String>) {
        message = msg.body()
        latch.countDown()
    }

    @ConsumeEvent("mutiny-message")
    fun mutinyMessage(msg: io.vertx.mutiny.core.eventbus.Message<String>) {
        message = msg.body()
        latch.countDown()
    }

    @ConsumeEvent("payload")
    fun payload(msg: String) {
        message = msg
        latch.countDown()
    }

    @ConsumeEvent("headers-payload")
    fun headersPayload(headers: io.vertx.core.MultiMap, msg: String) {
        message = "${headers["header"]} - $msg"
        latch.countDown()
    }

    @ConsumeEvent("mutiny-headers-payload")
    fun mutinyHeadersPayload(headers: io.vertx.mutiny.core.MultiMap, msg: String) {
        message = "${headers["header"]} - $msg"
        latch.countDown()
    }
}
