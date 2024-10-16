package io.quarkus.it.vertx.kotlin

import io.quarkus.test.junit.QuarkusTest
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus
import jakarta.inject.Inject
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class KotlinSuspendConsumeEventTest {
    @Inject lateinit var bus: EventBus

    @BeforeEach
    fun setup() {
        SuspendMessageConsumers.reset()
    }

    @Test
    fun message() {
        bus.send("suspend-message", "message")

        SuspendMessageConsumers.latch.await(2, TimeUnit.SECONDS)
        assertEquals("message", SuspendMessageConsumers.message)
    }

    @Test
    fun mutinyMessage() {
        bus.send("suspend-mutiny-message", "mutiny message")

        SuspendMessageConsumers.latch.await(2, TimeUnit.SECONDS)
        assertEquals("mutiny message", SuspendMessageConsumers.message)
    }

    @Test
    fun payload() {
        bus.send("suspend-payload", "just payload")

        SuspendMessageConsumers.latch.await(2, TimeUnit.SECONDS)
        assertEquals("just payload", SuspendMessageConsumers.message)
    }

    @Test
    fun headersPayload() {
        val options = DeliveryOptions().addHeader("header", "test header")
        bus.send("suspend-headers-payload", "payload", options)

        SuspendMessageConsumers.latch.await(2, TimeUnit.SECONDS)
        assertEquals("test header - payload", SuspendMessageConsumers.message)
    }

    @Test
    fun mutinyHeadersPayload() {
        val options = DeliveryOptions().addHeader("header", "test mutiny header")
        bus.send("suspend-mutiny-headers-payload", "payload", options)

        SuspendMessageConsumers.latch.await(2, TimeUnit.SECONDS)
        assertEquals("test mutiny header - payload", SuspendMessageConsumers.message)
    }
}
