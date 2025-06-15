package io.quarkus.arc.test.interceptors

import io.quarkus.arc.Arc
import io.quarkus.arc.test.ArcTestContainer
import jakarta.annotation.Priority
import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InterceptorBinding
import jakarta.interceptor.InvocationContext
import kotlin.test.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class InterceptableSuspendMethodTest {
    @RegisterExtension
    val container =
        ArcTestContainer(
            MyInterceptorBinding::class.java,
            MyInterceptor::class.java,
            MyService::class.java,
        )

    @Test
    fun test() {
        val service = Arc.container().instance(MyService::class.java).get()
        val result = runBlocking { service.hello() }

        assertEquals("hello", result)
        assertEquals(1, MyInterceptor.intercepted)
    }

    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    @InterceptorBinding
    annotation class MyInterceptorBinding

    @MyInterceptorBinding
    @Priority(1)
    @Interceptor
    class MyInterceptor {
        companion object {
            var intercepted = 0
        }

        @AroundInvoke
        fun intercept(ctx: InvocationContext): Any {
            intercepted++
            return ctx.proceed()
        }
    }

    @Singleton
    open class MyService {
        @MyInterceptorBinding
        open suspend fun hello(): String {
            delay(10)
            return "hello"
        }
    }
}
