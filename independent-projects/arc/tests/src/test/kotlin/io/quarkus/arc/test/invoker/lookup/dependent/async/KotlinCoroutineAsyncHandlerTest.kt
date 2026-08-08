package io.quarkus.arc.test.invoker.lookup.dependent.async

import io.quarkus.arc.Arc
import io.quarkus.arc.test.ArcTestContainer
import io.quarkus.arc.test.invoker.InvokerHelper
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar
import io.quarkus.arc.test.invoker.KotlinCoroutineAsyncHandler
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.invoke.Invoker
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

class KotlinCoroutineAsyncHandlerTest {
    @RegisterExtension
    val container: ArcTestContainer = ArcTestContainer.builder()
        .asyncHandler(KotlinCoroutineAsyncHandler::class.java)
        .beanClasses(MyDependency::class.java, MyService::class.java)
        .beanRegistrars(InvokerHelperRegistrar(MyService::class.java) { bean, factory, invokers ->
            for (name in listOf("helloSync", "helloAsync", "helloThrow")) {
                val method = bean.implClazz.firstMethod(name)
                invokers[name] = factory.createInvoker(bean, method)
                    .withInstanceLookup()
                    .withArgumentLookup(0)
                    .build()
            }
        })
        .build()

    private suspend fun <R> callSuspendInvoker(
        invoker: Invoker<MyService, R>,
        vararg args: Any?
    ): R {
        return suspendCoroutine { continuation ->
            val result = invoker.invoke(null, arrayOf(*args, continuation))
            if (result !== COROUTINE_SUSPENDED) {
                continuation.resume(result)
            }
        }
    }

    @Test
    fun testSync() {
        MyDependency.reset()

        val helper = Arc.container().instance(InvokerHelper::class.java).get()
        val invoker = helper.getInvoker<MyService, String>("helloSync")

        var result: Result<String>? = null
        val block = suspend {
            callSuspendInvoker(invoker, null)
        }
        block.startCoroutine(Continuation(EmptyCoroutineContext) {
            result = it
        })

        assertEquals(1, MyDependency.destroyedCounter.get())
        assertEquals("hello", result?.getOrNull())
    }

    @Test
    fun testAsync() {
        MyDependency.reset()

        val helper = Arc.container().instance(InvokerHelper::class.java).get()
        val invoker = helper.getInvoker<MyService, String>("helloAsync")
        val future = CompletableFuture<String>()

        var result: Result<String>? = null
        val block = suspend {
            callSuspendInvoker(invoker, null, future)
        }
        block.startCoroutine(Continuation(EmptyCoroutineContext) {
            result = it
        })

        assertEquals(0, MyDependency.destroyedCounter.get())
        assertNull(result)

        future.complete("hello")

        assertEquals(1, MyDependency.destroyedCounter.get())
        assertEquals("hello", result?.getOrNull())
    }

    @Test
    fun testSyncThrow() {
        MyDependency.reset()

        val helper = Arc.container().instance(InvokerHelper::class.java).get()
        val invoker = helper.getInvoker<MyService, String>("helloThrow")

        var result: Result<String>? = null
        val block = suspend {
            callSuspendInvoker(invoker, null)
        }
        block.startCoroutine(Continuation(EmptyCoroutineContext) {
            result = it
        })

        assertEquals(1, MyDependency.destroyedCounter.get())
        assertNotNull(result)
        assertTrue(result!!.isFailure)
        assertInstanceOf(IllegalArgumentException::class.java, result.exceptionOrNull())
    }

    @Dependent
    open class MyDependency {
        companion object {
            val destroyedCounter = AtomicInteger(0)

            fun reset() {
                destroyedCounter.set(0)
            }
        }

        @PreDestroy
        fun destroy() {
            destroyedCounter.incrementAndGet()
        }
    }

    @ApplicationScoped
    open class MyService {
        open suspend fun helloSync(dep: MyDependency): String {
            return "hello"
        }

        open suspend fun helloAsync(dep: MyDependency, future: CompletableFuture<String>): String {
            return future.await()
        }

        open suspend fun helloThrow(dep: MyDependency): String {
            throw IllegalArgumentException("sync throw")
        }
    }
}
