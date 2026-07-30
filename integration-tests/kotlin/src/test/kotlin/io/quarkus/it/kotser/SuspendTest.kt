package io.quarkus.it.kotser

import io.quarkus.test.junit.QuarkusTest
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

@QuarkusTest
class SuspendTest {

    @Test
    suspend fun testSuspendFunction() {
        delay(10)
        assertEquals(39, 30 + 9)
    }

    @Test
    suspend fun testSuspendFunctionException() {
        try {
            throwAfterDelay()
            fail("Expected exception")
        } catch (exception: IllegalStateException) {
            assertEquals("boom", exception.message)
        }
    }

    private suspend fun throwAfterDelay() {
        delay(10)
        throw IllegalStateException("boom")
    }
}
