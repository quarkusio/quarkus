package io.quarkus.arc.test.invoker.wrapper

import io.quarkus.arc.Arc
import io.quarkus.arc.test.ArcTestContainer
import io.quarkus.arc.test.invoker.InvokerHelper
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar
import io.quarkus.arc.test.invoker.KotlinCoroutineAsyncHandler
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import kotlinx.coroutines.async
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.invoke.Invoker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class KotlinSuspendUniWrapperTest {
    @RegisterExtension
    val container = ArcTestContainer.builder()
        .asyncHandler(KotlinCoroutineAsyncHandler::class.java)
        .beanClasses(MyDependency::class.java, MyService::class.java)
        .beanRegistrars(InvokerHelperRegistrar(MyService::class.java) { bean, factory, invokers ->
            for (name in listOf("helloSync", "helloAsync", "helloThrow")) {
                val method = bean.implClazz.firstMethod(name)
                invokers[name] = factory.createInvoker(bean, method)
                    .withInstanceLookup()
                    .withArgumentLookup(0)
                    .withInvocationWrapper(SuspendToUni::class.java, "wrap")
                    .build()
            }
        })
        .build()

    @Test
    fun testSync() {
        MyDependency.reset()

        val helper = Arc.container().instance(InvokerHelper::class.java).get()
        val invoker = helper.getInvoker<MyService, Uni<String>>("helloSync")

        val result = invoker.invoke(null, arrayOf(null))

        // the invoker offloads the method call to a thread pool, so we need to wait for completion
        assertEquals("hello", result.await().atMost(Duration.ofSeconds(5)))
        assertEquals(1, MyDependency.destroyedCounter.get())
    }

    @Test
    fun testAsync() {
        MyDependency.reset()

        val helper = Arc.container().instance(InvokerHelper::class.java).get()
        val invoker = helper.getInvoker<MyService, Uni<String>>("helloAsync")
        val future = CompletableFuture<String>()

        val result = invoker.invoke(null, arrayOf(null, future))

        // `Uni` is lazy, so the coroutine hasn't started yet
        assertEquals(0, MyDependency.destroyedCounter.get())

        future.complete("hello")

        // the invoker offloads the method call to a thread pool, so we need to wait for completion
        assertEquals("hello", result.await().atMost(Duration.ofSeconds(5)))
        assertEquals(1, MyDependency.destroyedCounter.get())
    }

    @Test
    fun testSyncThrow() {
        MyDependency.reset()

        val helper = Arc.container().instance(InvokerHelper::class.java).get()
        val invoker = helper.getInvoker<MyService, Uni<String>>("helloThrow")

        val result = invoker.invoke(null, arrayOf(null))

        // the invoker offloads the method call to a thread pool, so we need to wait for completion
        val exception = assertThrows(IllegalArgumentException::class.java) {
            result.await().atMost(Duration.ofSeconds(5))
        }
        assertEquals("sync throw", exception.message)
        assertEquals(1, MyDependency.destroyedCounter.get())
    }

    class SuspendToUni {
        companion object {
            val scope = CoroutineScope(SupervisorJob())

            @JvmStatic
            fun wrap(
                instance: Any?,
                arguments: Array<Any?>,
                invoker: Invoker<Any?, Any?>
            ): Uni<Any?> {
                // offloads to a thread pool (`Dispatchers.Default`, because we didn't specify any other one)
                return scope.async {
                    suspendCoroutine { continuation ->
                        // can't use `arguments.copyOf()` because the array component type is not necessarily `Object`
                        val newArgs = arrayOfNulls<Any>(arguments.size + 1)
                        arguments.copyInto(newArgs)
                        newArgs[newArgs.size - 1] = continuation
                        val result = invoker.invoke(instance, newArgs)
                        if (result !== COROUTINE_SUSPENDED) {
                            continuation.resume(result)
                        }
                    }
                }.asUni()
            }
        }
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
