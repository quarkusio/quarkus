package io.quarkus.it.testing.repro42000

import io.quarkus.test.junit.QuarkusTest
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@QuarkusTest
class Repro42000Test {
    companion object {
        val lambda: (String) -> String = { s -> s }

        fun function(s: String) = s

        @JvmStatic
        fun lambdaProvider(): Stream<Arguments> {
            return Stream.of(Arguments.of(lambda))
        }

        @JvmStatic
        fun functionProvider(): Stream<Arguments> {
            return Stream.of(Arguments.of(::function))
        }
    }

    @ParameterizedTest
    @MethodSource("lambdaProvider")
    fun testLambdaProvider(function: (String) -> String) {
        assertNotNull(function)
    }

    @ParameterizedTest
    @MethodSource("functionProvider")
    fun testFunctionProvider(function: (String) -> String) {
        assertNotNull(function)
    }
}
