package io.quarkus.arc.test.invoker.lookup.dependent.async

import io.quarkus.arc.Arc
import io.quarkus.arc.test.ArcTestContainer
import io.quarkus.arc.test.invoker.InvokerHelper
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar
import io.quarkus.arc.test.invoker.KotlinDeferredAsyncHandler
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Dependent
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class KotlinDeferredAsyncHandlerTest {
    @RegisterExtension
    val container: ArcTestContainer = ArcTestContainer.builder()
        .asyncHandler(KotlinDeferredAsyncHandler::class.java)
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

    @Test
    fun testSyncCompletion() {
        MyDependency.reset()

        val helper = Arc.container().instance(InvokerHelper::class.java).get()
        val invoker = helper.getInvoker<MyService, Deferred<String>>("helloSync")

        val result: Deferred<String> = invoker.invoke(null, arrayOf(null))

        assertEquals(1, MyDependency.destroyedCounter.get())
        assertTrue(result.isCompleted)
        assertEquals("hello", runBlocking {
            result.await()
        })
    }

    @Test
    fun testAsyncCompletion() {
        MyDependency.reset()

        val helper = Arc.container().instance(InvokerHelper::class.java).get()
        val invoker = helper.getInvoker<MyService, Deferred<String>>("helloAsync")
        val future = CompletableFuture<String>()

        val result: Deferred<String> = invoker.invoke(null, arrayOf(null, future))

        assertEquals(0, MyDependency.destroyedCounter.get())
        assertFalse(result.isCompleted)

        future.complete("hello")

        assertEquals(1, MyDependency.destroyedCounter.get())
        assertTrue(result.isCompleted)
        assertEquals("hello", runBlocking {
            result.await()
        })
    }

    @Test
    fun testSyncThrow() {
        MyDependency.reset()

        val helper = Arc.container().instance(InvokerHelper::class.java).get()
        val invoker = helper.getInvoker<MyService, Deferred<String>>("helloThrow")

        assertThrows(IllegalArgumentException::class.java) {
            invoker.invoke(null, arrayOf(null))
        }

        assertEquals(1, MyDependency.destroyedCounter.get())
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
        open fun helloSync(dep: MyDependency): Deferred<String> {
            return CompletableDeferred("hello")
        }

        open fun helloAsync(dep: MyDependency, future: CompletableFuture<String>): Deferred<String> {
            val deferred = CompletableDeferred<String>()
            future.whenComplete { value, error ->
                if (error == null) {
                    deferred.complete(value)
                } else {
                    deferred.completeExceptionally(error)
                }
            }
            return deferred
        }

        open fun helloThrow(dep: MyDependency): Deferred<String> {
            throw IllegalArgumentException("synchronous throw")
        }
    }
}
