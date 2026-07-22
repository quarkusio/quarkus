package io.quarkus.arc.test

import io.quarkus.arc.processor.KotlinUtils
import org.jboss.jandex.Index
import org.jboss.jandex.Type
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KotlinSuspendMethodTest {
    private val index = Index.of(Base::class.java, Impl::class.java)

    @Test
    fun shouldThrowOnSuspendMethodWithNonParameterizedContinuation() {
        val method = index.getClassByName(Impl::class.java)
            .methods()
            .first { it.name() == "update" && it.parameterTypes().last().kind() != Type.Kind.PARAMETERIZED_TYPE }

        assertFailsWith(IllegalArgumentException::class) {
            KotlinUtils.isKotlinSuspendMethod(method)
        }
    }

    @Test
    fun shouldRecognizeMethodWithParameterizedContinuationAndProduceCorrectReturnType() {
        val method = index.getClassByName(Impl::class.java)
            .methods()
            .first { it.name() == "update" && it.parameterTypes().last().kind() == Type.Kind.PARAMETERIZED_TYPE }

        assertTrue(KotlinUtils.isKotlinSuspendMethod(method))

        assertEquals("java.lang.String", KotlinUtils.getKotlinSuspendMethodResult(method).name().toString())
    }
}

abstract class Base<T : Any> {
    open suspend fun update(obj: T): T = throw UnsupportedOperationException()
}

class Impl : Base<String>() {
    override suspend fun update(obj: String): String = obj
}
