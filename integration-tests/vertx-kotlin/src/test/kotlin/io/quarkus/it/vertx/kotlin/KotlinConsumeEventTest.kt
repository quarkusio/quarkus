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
class KotlinConsumeEventTest {
    @Inject lateinit var bus: EventBus

    @BeforeEach
    fun setup() {
        MessageConsumers.reset()
    }

    @Test
    fun message() {
        bus.send("message", "message")

        MessageConsumers.latch.await(2, TimeUnit.SECONDS)
        assertEquals("message", MessageConsumers.message)
    }

    @Test
    fun mutinyMessage() {
        bus.send("mutiny-message", "mutiny message")

        MessageConsumers.latch.await(2, TimeUnit.SECONDS)
        assertEquals("mutiny message", MessageConsumers.message)
    }

    @Test
    fun payload() {
        bus.send("payload", "just payload")

        MessageConsumers.latch.await(2, TimeUnit.SECONDS)
        assertEquals("just payload", MessageConsumers.message)
    }

    @Test
    fun headersPayload() {
        val options = DeliveryOptions().addHeader("header", "test header")
        bus.send("headers-payload", "payload", options)

        MessageConsumers.latch.await(2, TimeUnit.SECONDS)
        assertEquals("test header - payload", MessageConsumers.message)
    }

    @Test
    fun mutinyHeadersPayload() {
        val options = DeliveryOptions().addHeader("header", "test mutiny header")
        bus.send("mutiny-headers-payload", "payload", options)

        MessageConsumers.latch.await(2, TimeUnit.SECONDS)
        assertEquals("test mutiny header - payload", MessageConsumers.message)
    }
}
