package io.quarkus.arc.kotlin

import io.quarkus.arc.Arc
import io.quarkus.test.QuarkusUnitTest
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.extension.RegisterExtension

class RequestContextCoroutineContextTest {

    companion object {
        @RegisterExtension
        @JvmStatic
        val TEST =
            QuarkusUnitTest().withApplicationRoot { jar: JavaArchive ->
                jar.addClasses(RequestData::class.java)
            }
    }

    @Inject lateinit var requestData: RequestData

    lateinit var expectedClassLoader: ClassLoader

    @BeforeEach
    @AfterEach
    fun setUp() {
        if (Arc.container().requestContext().isActive) {
            Arc.container().requestContext().terminate()
        }
        this.expectedClassLoader = Thread.currentThread().contextClassLoader
    }

    private fun assertThatCallersClassLoaderIsExpected() {
        Assertions.assertEquals(
            expectedClassLoader,
            Thread.currentThread().contextClassLoader,
            "Thread context class loader should be the expected one",
        )
    }

    @Test
    fun `without an active request scope on withContext`() {
        // GIVEN no active request context
        Assertions.assertFalse(
            Arc.container().requestContext().isActive,
            "Request context should not be active",
        )

        // WHEN we run a block
        runTest {
            withContext(Dispatchers.IO.withCdiContext()) {
                // THEN the request context should not be active
                Assertions.assertFalse(
                    Arc.container().requestContext().isActive,
                    "Request context should not be active",
                )

                assertThatCallersClassLoaderIsExpected()
            }
        }
    }

    @Test
    fun `without an active request scope on async`() {
        // GIVEN no active request context
        Assertions.assertFalse(
            Arc.container().requestContext().isActive,
            "Request context should not be active",
        )

        // WHEN we run a block with async
        runTest {
            coroutineScope {
                async(Dispatchers.IO.withCdiContext()) {
                        // THEN the request context should not be active
                        Assertions.assertFalse(
                            Arc.container().requestContext().isActive,
                            "Request context should not be active",
                        )

                        assertThatCallersClassLoaderIsExpected()
                    }
                    .await()
            }
        }
    }

    @Test
    fun `with an active request scope on withContext`() {
        // GIVEN an active request context
        Arc.container().requestContext().activate()
        Assertions.assertTrue(
            Arc.container().requestContext().isActive,
            "Request context should be active",
        )

        // AND a given number and an expected post-async number
        val givenNumber = 1234L
        val expectedPostAsyncNumber = 5432L

        // AND we set the number value in the request data
        requestData.numberValue = givenNumber

        // WHEN we run a block with the request context
        runTest {
            withContext(Dispatchers.IO.withCdiContext()) {
                // THEN the request context should be active
                Assertions.assertTrue(
                    Arc.container().requestContext().isActive,
                    "Request context should be active",
                )

                // AND the number value should match the given number
                Assertions.assertEquals(givenNumber, requestData.numberValue)

                assertThatCallersClassLoaderIsExpected()

                // AND the number value should match the given number after a short delay
                delay(10)
                Assertions.assertEquals(givenNumber, requestData.numberValue)

                assertThatCallersClassLoaderIsExpected()

                // WHEN we set the number value to the expected post-async number
                requestData.numberValue = expectedPostAsyncNumber

                // THEN the number value should match the expected post-async number after a short
                // delay
                delay(10)
                Assertions.assertEquals(expectedPostAsyncNumber, requestData.numberValue)

                assertThatCallersClassLoaderIsExpected()
            }
        }

        // THEN the number value should match the expected post-async number after the block
        // execution
        Assertions.assertEquals(expectedPostAsyncNumber, requestData.numberValue)
    }

    @Test
    fun `with an active request scope on async`() {
        // GIVEN an active request context
        Arc.container().requestContext().activate()
        Assertions.assertTrue(
            Arc.container().requestContext().isActive,
            "Request context should be active",
        )

        // AND a given number and an expected post-async number
        val givenNumber = 1234L
        val expectedPostAsyncNumber = 5432L

        // AND we set the number value in the request data
        requestData.numberValue = givenNumber

        // WHEN we run a block with async
        runTest {
            coroutineScope {
                async(Dispatchers.IO.withCdiContext()) {
                        // THEN the request context should be active
                        Assertions.assertTrue(
                            Arc.container().requestContext().isActive,
                            "Request context should be active",
                        )

                        // AND the number value should match the given number
                        Assertions.assertEquals(givenNumber, requestData.numberValue)

                        assertThatCallersClassLoaderIsExpected()

                        // AND the number value should match the given number after a short delay
                        delay(10)
                        Assertions.assertEquals(givenNumber, requestData.numberValue)

                        assertThatCallersClassLoaderIsExpected()

                        // WHEN we set the number value to the expected post-async number
                        requestData.numberValue = expectedPostAsyncNumber

                        // THEN the number value should match the expected post-async number after a
                        // short delay
                        delay(10)
                        Assertions.assertEquals(expectedPostAsyncNumber, requestData.numberValue)

                        assertThatCallersClassLoaderIsExpected()
                    }
                    .await()
            }
        }

        // THEN the number value should match the expected post-async number after the block
        // execution
        Assertions.assertEquals(expectedPostAsyncNumber, requestData.numberValue)
    }

    @Test
    fun `with an active request scope on inner coroutine scope in async`() {
        // GIVEN an active request context
        Arc.container().requestContext().activate()
        Assertions.assertTrue(
            Arc.container().requestContext().isActive,
            "Request context should be active",
        )

        // AND a given number and an expected post-async number
        val givenNumber = 1234L
        val expectedPostAsyncNumber = 5432L

        // AND we set the number value in the request data
        requestData.numberValue = givenNumber

        // WHEN we run a block with async
        runTest {
            coroutineScope {
                async(Dispatchers.IO.withCdiContext()) {
                        coroutineScope {
                            // THEN the request context should be active
                            Assertions.assertTrue(
                                Arc.container().requestContext().isActive,
                                "Request context should be active",
                            )

                            // AND the number value should match the given number
                            Assertions.assertEquals(givenNumber, requestData.numberValue)

                            assertThatCallersClassLoaderIsExpected()

                            // AND the number value should match the given number after a short
                            // delay
                            delay(10)
                            Assertions.assertEquals(givenNumber, requestData.numberValue)

                            assertThatCallersClassLoaderIsExpected()

                            // WHEN we set the number value to the expected post-async number
                            requestData.numberValue = expectedPostAsyncNumber

                            // THEN the number value should match the expected post-async number
                            // after a short delay
                            delay(10)
                            Assertions.assertEquals(
                                expectedPostAsyncNumber,
                                requestData.numberValue,
                            )

                            assertThatCallersClassLoaderIsExpected()
                        }
                    }
                    .await()
            }
        }

        // THEN the number value should match the expected post-async number after the block
        // execution
        Assertions.assertEquals(expectedPostAsyncNumber, requestData.numberValue)
    }

    @Test
    fun `with a terminated request scope while on async (undefined behavior)`() {
        // GIVEN an active request context
        Arc.container().requestContext().activate()
        Assertions.assertTrue(
            Arc.container().requestContext().isActive,
            "Request context should be active",
        )

        // AND a given number
        val givenNumber = 1234L

        // AND we set the number value in the request data
        requestData.numberValue = givenNumber

        val asyncStarted = CompletableDeferred<Unit>()
        val requestScopeTerminated = CompletableDeferred<Unit>()

        // WHEN we run a block with async
        runTest {
            val job = launch {
                async(Dispatchers.IO.withCdiContext()) {
                        // THEN the request context should be active
                        Assertions.assertTrue(
                            Arc.container().requestContext().isActive,
                            "Request context should be active",
                        )

                        // AND the number value should match the given number
                        Assertions.assertEquals(givenNumber, requestData.numberValue)

                        assertThatCallersClassLoaderIsExpected()

                        asyncStarted.complete(Unit)

                        // WHEN we wait for the request context to be terminated by another thread
                        requestScopeTerminated.await()

                        // THEN the request context should not be active (undefined behavior)
                        Assertions.assertFalse(
                            Arc.container().requestContext().isActive,
                            "Request context should not be active",
                        )
                    }
                    .await()
            }

            launch {
                asyncStarted.await()

                Assertions.assertTrue(
                    Arc.container().requestContext().isActive,
                    "Request context should be active",
                )
                Assertions.assertEquals(givenNumber, requestData.numberValue)
                Arc.container().requestContext().terminate()
                Assertions.assertFalse(
                    Arc.container().requestContext().isActive,
                    "Request context should be active",
                )

                requestScopeTerminated.complete(Unit)
            }

            job.join()
        }
    }

    @Test
    fun `with two active request scope on async on same coroutine`() {
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
        Assertions.assertNotEquals(firstGivenNumber, requestData.numberValue)

        val secondGivenNumber = 5432L
        val expectedSecondGivenNumber = 95432L
        requestData.numberValue = secondGivenNumber

        Assertions.assertNotEquals(firstRequestState, secondRequestState)

        val waitForFirstEnd = CompletableDeferred<Unit>()
        val waitForSecondMiddle = CompletableDeferred<Unit>()
        val waitForSecondEnd = CompletableDeferred<Unit>()

        // WHEN we run a block with async
        runTest {
            val jobFirstRequest = launch {
                Arc.container().requestContext().activate(firstRequestState)
                async(Dispatchers.IO.withCdiContext()) {
                        // THEN the request context should be active
                        Assertions.assertTrue(
                            Arc.container().requestContext().isActive,
                            "Request context should be active",
                        )

                        // AND the number value should match the given number
                        Assertions.assertEquals(firstGivenNumber, requestData.numberValue)

                        assertThatCallersClassLoaderIsExpected()

                        waitForSecondMiddle.await()

                        // THEN the request context should be active
                        Assertions.assertTrue(
                            Arc.container().requestContext().isActive,
                            "Request context should be active",
                        )

                        // AND the number value should match the given number
                        Assertions.assertEquals(firstGivenNumber, requestData.numberValue)

                        assertThatCallersClassLoaderIsExpected()

                        requestData.numberValue = expectedFirstGivenNumber

                        waitForFirstEnd.complete(Unit)
                    }
                    .await()
            }

            val jobSecondRequest = launch {
                Arc.container().requestContext().activate(secondRequestState)
                async(Dispatchers.IO.withCdiContext()) {
                        // THEN the request context should be active
                        Assertions.assertTrue(
                            Arc.container().requestContext().isActive,
                            "Request context should be active",
                        )

                        // AND the number value should match the given number
                        Assertions.assertEquals(secondGivenNumber, requestData.numberValue)

                        assertThatCallersClassLoaderIsExpected()

                        waitForSecondMiddle.complete(Unit)

                        waitForFirstEnd.await()

                        // THEN the request context should be active
                        Assertions.assertTrue(
                            Arc.container().requestContext().isActive,
                            "Request context should be active",
                        )

                        // AND the number value should match the given number
                        Assertions.assertEquals(secondGivenNumber, requestData.numberValue)

                        assertThatCallersClassLoaderIsExpected()

                        requestData.numberValue = expectedSecondGivenNumber

                        waitForSecondEnd.complete(Unit)
                    }
                    .await()
            }

            jobSecondRequest.join()
            jobFirstRequest.join()
        }

        Arc.container().requestContext().activate(secondRequestState)
        Assertions.assertEquals(expectedSecondGivenNumber, requestData.numberValue)
        Arc.container().requestContext().terminate()

        Arc.container().requestContext().activate(firstRequestState)
        Assertions.assertEquals(expectedFirstGivenNumber, requestData.numberValue)
        Arc.container().requestContext().terminate()
    }

    @RequestScoped
    class RequestData {
        var numberValue = 0L
    }
}
