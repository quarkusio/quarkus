package io.quarkus.arc.test.interceptors

import io.quarkus.arc.Arc
import io.quarkus.arc.test.ArcTestContainer
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InterceptorBinding
import jakarta.interceptor.InvocationContext
import java.io.IOException
import java.util.Locale
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class KotlinInterceptorTest {
    @RegisterExtension
    val container =
        ArcTestContainer(
            Converter::class.java,
            ToUpperCaseConverter::class.java,
            FailingInterceptor::class.java,
            AlwaysFail::class.java,
        )

    @Test
    fun testInterceptionThrowsUnwrapped() {
        val converter = Arc.container().instance(ToUpperCaseConverter::class.java).get()
        assertFailsWith<IOException> { converter.convert("holA!") }
    }

    interface Converter<T> {
        fun convert(value: T): T
    }

    @AlwaysFail
    @ApplicationScoped
    open class ToUpperCaseConverter : Converter<String> {
        override fun convert(value: String): String {
            return value.uppercase(Locale.getDefault())
        }
    }

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    @InterceptorBinding
    annotation class AlwaysFail

    @AlwaysFail
    @Priority(1)
    @Interceptor
    class FailingInterceptor {
        @AroundInvoke
        fun fail(ctx: InvocationContext): Any {
            throw IOException()
        }
    }
}
