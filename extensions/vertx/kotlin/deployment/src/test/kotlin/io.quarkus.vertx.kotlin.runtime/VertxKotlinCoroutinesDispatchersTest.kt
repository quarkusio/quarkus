package io.quarkus.vertx.kotlin.runtime

import io.quarkus.arc.Arc
import io.quarkus.test.QuarkusUnitTest
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class VertxKotlinCoroutinesDispatchersTest {

    companion object {
        @RegisterExtension
        @JvmStatic
        val TEST =
            QuarkusUnitTest().withApplicationRoot { jar ->
                jar.addClasses(RequestData::class.java)
            }
    }

    enum class DispatcherType(
        val dispatcherProvider: (VertxKotlinCoroutinesDispatchers) -> CoroutineDispatcher
    ) {
        BLOCKING({ it.contextualBlockingDispatcher() }),
        NON_BLOCKING({ it.contextualNonBlockingDispatcher() })
        ;


    }

    @Inject
    lateinit var requestData: RequestData

    @Inject
    lateinit var dispatchers: VertxKotlinCoroutinesDispatchers

    lateinit var expectedClassLoader: ClassLoader

    @BeforeEach
    @AfterEach
    fun setUp() {
        if (Arc.container().requestContext().isActive) {
            Arc.container().requestContext().terminate()
        }
        expectedClassLoader = Thread.currentThread().contextClassLoader
    }

    private fun assertThatCallersClassLoaderIsExpected() {
        assertEquals(
            expectedClassLoader,
            Thread.currentThread().contextClassLoader,
            "Thread context class loader should be the expected one"
        )
    }

    private fun testDispatcher(displayerType: String): CoroutineDispatcher {
        return DispatcherType.valueOf(displayerType).dispatcherProvider(dispatchers)
    }

    @ParameterizedTest
    @ValueSource(strings = ["BLOCKING", "NON_BLOCKING"])
    fun `without an active request scope on withContext`(dispatcherType: String) {
        // GIVEN no active request context
        assertFalse(Arc.container().requestContext().isActive, "Request context should not be active")

        // WHEN we run a block
        runTest {
            withContext(testDispatcher(dispatcherType)) {
                // THEN the request context should not be active
                assertFalse(Arc.container().requestContext().isActive, "Request context should not be active")

                assertThatCallersClassLoaderIsExpected()
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["BLOCKING", "NON_BLOCKING"])
    fun `without an active request scope on async`(dispatcherType: String) {
        // GIVEN no active request context
        assertFalse(Arc.container().requestContext().isActive, "Request context should not be active")

        // WHEN we run a block with async
        runTest {
            coroutineScope {
                async(testDispatcher(dispatcherType)) {
                    // THEN the request context should not be active
                    assertFalse(Arc.container().requestContext().isActive, "Request context should not be active")

                    assertThatCallersClassLoaderIsExpected()
                }.await()
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["BLOCKING", "NON_BLOCKING"])
    fun `with an active request scope on withContext`(dispatcherType: String) {
        // GIVEN an active request context
        Arc.container().requestContext().activate()
        assertTrue(Arc.container().requestContext().isActive, "Request context should be active")

        // AND a given number and an expected post-async number
        val givenNumber = 1234L
        val expectedPostAsyncNumber = 5432L

        // AND we set the number value in the request data
        requestData.numberValue = givenNumber

        // WHEN we run a block with the request context
        runTest {
            withContext(testDispatcher(dispatcherType)) {
                // THEN the request context should be active
                assertTrue(Arc.container().requestContext().isActive, "Request context should be active")

                // AND the number value should match the given number
                assertEquals(givenNumber, requestData.numberValue)

                assertThatCallersClassLoaderIsExpected()

                // AND the number value should match the given number after a short delay
                delay(10)
                assertEquals(givenNumber, requestData.numberValue)

                assertThatCallersClassLoaderIsExpected()

                // WHEN we set the number value to the expected post-async number
                requestData.numberValue = expectedPostAsyncNumber

                // THEN the number value should match the expected post-async number after a short delay
                delay(10)
                assertEquals(expectedPostAsyncNumber, requestData.numberValue)

                assertThatCallersClassLoaderIsExpected()
            }
        }

        // THEN the number value should match the expected post-async number after the block execution
        assertEquals(expectedPostAsyncNumber, requestData.numberValue)
    }

    @ParameterizedTest
    @ValueSource(strings = ["BLOCKING", "NON_BLOCKING"])
    fun `with an active request scope on async`(dispatcherType: String) {
        // GIVEN an active request context
        Arc.container().requestContext().activate()
        assertTrue(Arc.container().requestContext().isActive, "Request context should be active")

        // AND a given number and an expected post-async number
        val givenNumber = 1234L
        val expectedPostAsyncNumber = 5432L

        // AND we set the number value in the request data
        requestData.numberValue = givenNumber

        // WHEN we run a block with async
        runTest {
            coroutineScope {
                async(testDispatcher(dispatcherType)) {
                    // THEN the request context should be active
                    assertTrue(Arc.container().requestContext().isActive, "Request context should be active")

                    // AND the number value should match the given number
                    assertEquals(givenNumber, requestData.numberValue)

                    assertThatCallersClassLoaderIsExpected()

                    // AND the number value should match the given number after a short delay
                    delay(10)
                    assertEquals(givenNumber, requestData.numberValue)

                    assertThatCallersClassLoaderIsExpected()

                    // WHEN we set the number value to the expected post-async number
                    requestData.numberValue = expectedPostAsyncNumber

                    // THEN the number value should match the expected post-async number after a short delay
                    delay(10)
                    assertEquals(expectedPostAsyncNumber, requestData.numberValue)

                    assertThatCallersClassLoaderIsExpected()
                }.await()
            }
        }

        // THEN the number value should match the expected post-async number after the block execution
        assertEquals(expectedPostAsyncNumber, requestData.numberValue)
    }

    @ParameterizedTest
    @ValueSource(strings = ["BLOCKING", "NON_BLOCKING"])
    fun `with an active request scope on inner coroutine scope in async`(dispatcherType: String) {
        // GIVEN an active request context
        Arc.container().requestContext().activate()
        assertTrue(Arc.container().requestContext().isActive, "Request context should be active")

        // AND a given number and an expected post-async number
        val givenNumber = 1234L
        val expectedPostAsyncNumber = 5432L

        // AND we set the number value in the request data
        requestData.numberValue = givenNumber

        // WHEN we run a block with async
        runTest {
            coroutineScope {
                async(testDispatcher(dispatcherType)) {
                    coroutineScope {
                        // THEN the request context should be active
                        assertTrue(Arc.container().requestContext().isActive, "Request context should be active")

                        // AND the number value should match the given number
                        assertEquals(givenNumber, requestData.numberValue)

                        assertThatCallersClassLoaderIsExpected()

                        // AND the number value should match the given number after a short delay
                        delay(10)
                        assertEquals(givenNumber, requestData.numberValue)

                        assertThatCallersClassLoaderIsExpected()

                        // WHEN we set the number value to the expected post-async number
                        requestData.numberValue = expectedPostAsyncNumber

                        // THEN the number value should match the expected post-async number after a short delay
                        delay(10)
                        assertEquals(expectedPostAsyncNumber, requestData.numberValue)

                        assertThatCallersClassLoaderIsExpected()
                    }
                }.await()
            }
        }

        // THEN the number value should match the expected post-async number after the block execution
        assertEquals(expectedPostAsyncNumber, requestData.numberValue)
    }

    @ParameterizedTest
    @ValueSource(strings = ["BLOCKING", "NON_BLOCKING"])
    fun `with a terminated request scope while on async (undefined behavior)`(dispatcherType: String) {
        // GIVEN an active request context
        Arc.container().requestContext().activate()
        assertTrue(Arc.container().requestContext().isActive, "Request context should be active")

        // AND a given number
        val givenNumber = 1234L

        // AND we set the number value in the request data
        requestData.numberValue = givenNumber

        val asyncStarted = CompletableDeferred<Unit>()
        val requestScopeTerminated = CompletableDeferred<Unit>()

        // WHEN we run a block with async
        runTest {
            val job = launch {
                async(testDispatcher(dispatcherType)) {
                    // THEN the request context should be active
                    assertTrue(Arc.container().requestContext().isActive, "Request context should be active")

                    // AND the number value should match the given number
                    assertEquals(givenNumber, requestData.numberValue)

                    assertThatCallersClassLoaderIsExpected()

                    asyncStarted.complete(Unit)

                    // WHEN we wait for the request context to be terminated by another thread
                    requestScopeTerminated.await()

                    // THEN the request context should not be active (undefined behavior)
                    assertFalse(Arc.container().requestContext().isActive, "Request context should not be active")
                }.await()

            }

            launch {
                asyncStarted.await()

                assertTrue(Arc.container().requestContext().isActive, "Request context should be active")
                assertEquals(givenNumber, requestData.numberValue)
                Arc.container().requestContext().terminate()
                assertFalse(Arc.container().requestContext().isActive, "Request context should be active")

                requestScopeTerminated.complete(Unit)
            }

            job.join()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["BLOCKING", "NON_BLOCKING"])
    fun `with two active request scope on async on same coroutine`(dispatcherType: String) {
        // GIVEN two active request contexts with different giveNumbers
        Arc.container().requestContext().activate()

        val firstRequestState = Arc.container().requestContext().stateIfActive
        assertNotNull(firstRequestState)

        val firstGivenNumber = 1234L
        val expectedFirstGivenNumber = 91234L
        requestData.numberValue = firstGivenNumber

        Arc.container().requestContext().activate()

        val secondRequestState = Arc.container().requestContext().stateIfActive
        assertNotNull(secondRequestState)
        assertNotEquals(firstGivenNumber, requestData.numberValue)

        val secondGivenNumber = 5432L
        val expectedSecondGivenNumber = 95432L
        requestData.numberValue = secondGivenNumber

        assertNotEquals(firstRequestState, secondRequestState)

        val waitForFirstEnd = CompletableDeferred<Unit>()
        val waitForSecondMiddle = CompletableDeferred<Unit>()
        val waitForSecondEnd = CompletableDeferred<Unit>()

        // WHEN we run a block with async
        runTest {

            val jobFirstRequest = launch {
                Arc.container().requestContext().activate(firstRequestState)
                async(testDispatcher(dispatcherType)) {
                    // THEN the request context should be active
                    assertTrue(Arc.container().requestContext().isActive, "Request context should be active")

                    // AND the number value should match the given number
                    assertEquals(firstGivenNumber, requestData.numberValue)

                    assertThatCallersClassLoaderIsExpected()

                    waitForSecondMiddle.await()

                    // THEN the request context should be active
                    assertTrue(Arc.container().requestContext().isActive, "Request context should be active")

                    // AND the number value should match the given number
                    assertEquals(firstGivenNumber, requestData.numberValue)

                    assertThatCallersClassLoaderIsExpected()

                    requestData.numberValue = expectedFirstGivenNumber

                    waitForFirstEnd.complete(Unit)

                }.await()
            }

            val jobSecondRequest = launch {
                Arc.container().requestContext().activate(secondRequestState)
                async(testDispatcher(dispatcherType)) {
                    // THEN the request context should be active
                    assertTrue(Arc.container().requestContext().isActive, "Request context should be active")

                    // AND the number value should match the given number
                    assertEquals(secondGivenNumber, requestData.numberValue)

                    assertThatCallersClassLoaderIsExpected()

                    waitForSecondMiddle.complete(Unit)

                    waitForFirstEnd.await()

                    // THEN the request context should be active
                    assertTrue(Arc.container().requestContext().isActive, "Request context should be active")

                    // AND the number value should match the given number
                    assertEquals(secondGivenNumber, requestData.numberValue)

                    assertThatCallersClassLoaderIsExpected()

                    requestData.numberValue = expectedSecondGivenNumber

                    waitForSecondEnd.complete(Unit)

                }.await()
            }


            jobSecondRequest.join()
            jobFirstRequest.join()
        }

        Arc.container().requestContext().activate(secondRequestState)
        assertEquals(expectedSecondGivenNumber, requestData.numberValue)
        Arc.container().requestContext().terminate()

        Arc.container().requestContext().activate(firstRequestState)
        assertEquals(expectedFirstGivenNumber, requestData.numberValue)
        Arc.container().requestContext().terminate()
    }

    @ParameterizedTest
    @ValueSource(strings = ["BLOCKING", "NON_BLOCKING"])
    fun `exception on withContext`(dispatcherType: String) {
        runTest {
            val message = "Some Error Message"

            val ex = assertThrows<IllegalArgumentException> {
                withContext(testDispatcher(dispatcherType)) {
                    throw IllegalArgumentException(message)
                }
            }
            assertEquals(message, ex.message)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["BLOCKING", "NON_BLOCKING"])
    fun `exception on async`(dispatcherType: String) {
        val message = "Some Error Message"
        val ex = assertThrows<IllegalArgumentException> {
            runTest {
                val deferred = async(testDispatcher(dispatcherType)) {
                    throw IllegalArgumentException(message)
                }

                val ex = assertThrows<IllegalArgumentException> { deferred.await() }
                assertEquals(message, ex.message)
            }
        }
        assertEquals(message, ex.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["BLOCKING", "NON_BLOCKING"])
    fun `call method that requires active request context (45441 reproducer)`(dispatcherType: String) {
        runTest {
            // GIVEN an active request context
            Arc.container().requestContext().activate()
            assertTrue(Arc.container().requestContext().isActive, "Request context should be active")

            // AND a given number and
            val givenNumber = 1234L

            requestData.numberValue = givenNumber

            // WHEN we call a method that requires an active request context and that suspends the coroutine
            requestData.sleep()

            // THEN the request context should still be active
            assertTrue(Arc.container().requestContext().isActive, "Request context should be active")
            // AND the number value should match the given number
            assertEquals(givenNumber, requestData.numberValue)

            // WHEN we switch to a different dispatcher
            withContext(testDispatcher(dispatcherType)) {

                // THEN the request context should still be active
                assertTrue(Arc.container().requestContext().isActive, "Request context should be active")
                // AND the number value should match the given number
                assertEquals(givenNumber, requestData.numberValue)

                // WHEN we call a method that requires an active request context and that suspends the coroutine
                // THEN it should not throw an exception
                assertDoesNotThrow { requestData.sleep() }

                // AND the request context should still be active
                assertTrue(Arc.container().requestContext().isActive, "Request context should be active")
                // AND the number value should match the given number
                assertEquals(givenNumber, requestData.numberValue)
            }

            // WHEN we return to the call dispatcher
            // THEN the request context should still be active
            assertTrue(Arc.container().requestContext().isActive, "Request context should be active")
            // AND the number value should match the given number
            assertEquals(givenNumber, requestData.numberValue)
        }
    }


    @RequestScoped
    class RequestData {
        var numberValue = 0L

        suspend fun sleep() {
            delay(10)
        }
    }

}