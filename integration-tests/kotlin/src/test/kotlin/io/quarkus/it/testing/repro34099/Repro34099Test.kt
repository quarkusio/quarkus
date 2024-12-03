package io.quarkus.it.testing.repro34099

import io.quarkus.test.junit.QuarkusTest
import java.time.Duration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout

@QuarkusTest
class Repro34099Test {
    @Test
    fun javaAssertion() {
        Assertions.assertTimeout(Duration.ofSeconds(1)) {}
    }

    @Test
    @Disabled("https://github.com/quarkusio/quarkus/issues/34099")
    // fails with `Linkage loader constraint violation`
    fun kotlinAssertion() {
        assertTimeout(Duration.ofSeconds(1)) {}
    }
}
