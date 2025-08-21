package io.quarkus.smallrye.reactivemessaging

import io.quarkus.arc.Arc
import io.quarkus.smallrye.reactivemessaging.runtime.ContextualEmitter
import io.quarkus.test.QuarkusUnitTest
import io.smallrye.common.vertx.VertxContext
import io.vertx.core.Vertx
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class KotlinSubscriberTest {

    companion object {
        @RegisterExtension
        @JvmStatic
        val config: QuarkusUnitTest? = QuarkusUnitTest()
            .withApplicationRoot(Consumer { jar: JavaArchive? ->
                jar!!
                    .addClasses(
                        KotlinSubscriber::class.java
                    )
                    .addAsResource(
                        File("src/test/resources/config/worker-config.properties"),
                        "application.properties"
                    )
            })
    }

    @Inject
    lateinit var requestData: RequestData

    @Inject
    lateinit var kotlinSubscriber: KotlinSubscriber

    @Inject
    @Channel("contextual-in")
    lateinit var contextualEmitter: ContextualEmitter<String>

    @Inject
    @Channel("non-contextual-in")
    lateinit var nonContextualEmitter: Emitter<String>

    @Inject
    lateinit var vertx: Vertx

    @AfterEach
    fun cleanup() {
        kotlinSubscriber.reset()
        Arc.container().requestContext().terminate()
    }

    @Test
    fun `preserve request context on suspend when publisher doesn't have a request context`() {
        // GIVEN No request context is active
        Arc.container().requestContext().terminate()

        // AND a message
        val message = "preserve this context when no context on publisher"

        // AND a publisher's context (duplicated vertx context)
        val publisherContext = VertxContext.createNewDuplicatedContext(vertx.orCreateContext)

        // WHEN we publish a message through the emitter
        publisherContext.runOnContext {
            nonContextualEmitter.send(message)
        }

        // THEN the subscriber receives the message and preserves the request context
        kotlinSubscriber.verifyReceivedDataIsPreservedWhenSuspendingAndResuming(message)

    }

    @Test
    fun `preserve request context on suspend when publisher does have a request context`() {
        // GIVEN that request context is active
        val requestContext = Arc.container().requestContext()
        val state = requestContext.activate()

        // AND a message
        val message = "preserve this context when publisher has a request context"

        // AND a publisher's context (duplicated vertx context)
        val publisherContext = VertxContext.createNewDuplicatedContext(vertx.orCreateContext)
        publisherContext.runOnContext {
            requestContext.activate(state)
        }

        // WHEN we publish a message through the emitter
        publisherContext.runOnContext {
            contextualEmitter.send(message).subscribe().with { }
        }

        // THEN the subscriber receives the message and preserves the request context
        kotlinSubscriber.verifyReceivedDataIsPreservedWhenSuspendingAndResuming(message)

        // AND the request data is preserved in the test's context
        assertEquals(message, requestData.messageValue)

        // AND the request data is preserved in the publisher's context
        val messageInContext = CompletableFuture<String>()
        publisherContext.runOnContext {
            messageInContext.complete(requestData.messageValue)
        }
        assertEquals(message, messageInContext.get(30, TimeUnit.SECONDS))
    }

    @Test
    fun `preserve request context on suspend when publisher publishing to two request contexts`() {
        // GIVEN that request context is active
        val requestContext = Arc.container().requestContext()
        val states = listOf(requestContext.activate(), requestContext.activate())

        // AND a message
        val messages = listOf(
            "preserve this context when publisher has a request context 1",
            "preserve this context when publisher has a request context 2",
        )

        // AND a publisher's context (duplicated vertx context)
        val vertxContext = vertx.orCreateContext
        val publisherContexts = listOf(
            VertxContext.createNewDuplicatedContext(vertxContext),
            VertxContext.createNewDuplicatedContext(vertxContext),
        )
        states.forEachIndexed { index, state ->
            val publisherContext = publisherContexts[index]
            publisherContext.runOnContext {
                requestContext.activate(state)
            }
        }

        // WHEN we publish a message through the emitter on both contexts
        states.forEachIndexed { index, state ->
            val message = messages[index]
            val publisherContext = publisherContexts[index]
            publisherContext.runOnContext {
                contextualEmitter.send(message).subscribe().with { }
            }
        }

        states.forEachIndexed { index, state ->
            val message = messages[index]
            val publisherContext = publisherContexts[index]
            requestContext.activate(state)


            // THEN the subscriber receives the message and preserves the request context
            kotlinSubscriber.verifyReceivedDataIsPreservedWhenSuspendingAndResuming(message)

            // AND the request data is preserved in the test's context
            assertEquals(message, requestData.messageValue)

            // AND the request data is preserved in the publisher's context
            val messageInContext = CompletableFuture<String>()
            publisherContext.runOnContext {
                requestContext.activate(state)
                messageInContext.complete(requestData.messageValue)
            }
            assertEquals(message, messageInContext.get(30, TimeUnit.SECONDS))
        }

        // cleanup
        states.forEach { state ->
            requestContext.activate(state)
            requestContext.terminate()
        }
    }

    @RequestScoped
    class RequestData {
        var messageValue: String = ""

        suspend fun sleep() {
            delay(10)
        }
    }

    @RequestScoped
    class ConsumedRequestData {
        val messageValue = CompletableFuture<String>()
    }

    @ApplicationScoped
    class KotlinSubscriber @Inject constructor(
        private val requestData: RequestData,
        private val consumedRequestData: ConsumedRequestData
    ) {

        private val preSuspendReceivedData = LinkedBlockingQueue<String>()
        private val postSuspendReceivedData = LinkedBlockingQueue<String>()
        private val requestContext = Arc.container().requestContext()

        @Incoming("contextual-in")
        suspend fun consumeFromContextual(message: String) {
            return consume(message)
        }

        @Incoming("non-contextual-in")
        suspend fun consumeFromNonContextual(message: String) {
            return consume(message)
        }

        private suspend fun consume(message: String) {

            requestData.messageValue = message

            // capture the data before suspension
            preSuspendReceivedData.offer(requestData.messageValue)

            // force a suspension
            delay(10)

            // capture the data after suspension
            if (requestContext.isActive) {
                consumedRequestData.messageValue.complete(message)
            }
            postSuspendReceivedData.offer(requestData.messageValue)

        }

        fun reset() {
            preSuspendReceivedData.clear()
            postSuspendReceivedData.clear()
        }

        fun verifyReceivedDataIsPreservedWhenSuspendingAndResuming(
            expectedMessage: String
        ) {

            val preMessage = preSuspendReceivedData.poll(30, TimeUnit.SECONDS)
            assertEquals(expectedMessage, preMessage)

            val postMessage = postSuspendReceivedData.poll(30, TimeUnit.SECONDS)
            assertEquals(expectedMessage, postMessage)

            if (requestContext.isActive) {
                val consumedMessage = consumedRequestData.messageValue.get(30, TimeUnit.SECONDS)
                assertEquals(expectedMessage, consumedMessage)
            }
        }
    }
}