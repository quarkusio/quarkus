package io.quarkus.arc.test.interceptors

import io.quarkus.arc.test.ArcTestContainer
import jakarta.annotation.Priority
import jakarta.enterprise.inject.spi.DeploymentException
import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InterceptorBinding
import jakarta.interceptor.InvocationContext
import kotlin.test.assertContains
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class NoninterceptableSuspendMethodTest {
    @RegisterExtension
    val container =
        ArcTestContainer.builder()
            .beanClasses(
                MyInterceptorBinding::class.java,
                MyInterceptor::class.java,
                MyService::class.java,
            )
            .shouldFail()
            .build()

    @Test
    fun trigger() {
        val error = container.failure
        assertNotNull(error)
        assertIs<DeploymentException>(error)
        assertContains(
            error.message!!,
            "Kotlin `suspend` functions must be `open` and declared in `open` classes, otherwise they cannot be intercepted",
        )
    }

    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    @InterceptorBinding
    annotation class MyInterceptorBinding

    @MyInterceptorBinding
    @Priority(1)
    @Interceptor
    class MyInterceptor {
        @AroundInvoke
        fun intercept(ctx: InvocationContext): Any {
            return ctx.proceed()
        }
    }

    @Singleton
    class MyService {
        @MyInterceptorBinding
        suspend fun hello(): String {
            delay(10)
            return "hello"
        }
    }
}
