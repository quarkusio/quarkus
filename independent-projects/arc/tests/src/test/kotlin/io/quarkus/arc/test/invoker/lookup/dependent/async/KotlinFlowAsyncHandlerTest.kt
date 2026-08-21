package io.quarkus.arc.test.invoker.lookup.dependent.async

import io.quarkus.arc.Arc
import io.quarkus.arc.test.ArcTestContainer
import io.quarkus.arc.test.invoker.InvokerHelper
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar
import io.quarkus.arc.test.invoker.KotlinFlowAsyncHandler
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Dependent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.concurrent.atomic.AtomicInteger

class KotlinFlowAsyncHandlerTest {
    @RegisterExtension
    val container: ArcTestContainer = ArcTestContainer.builder()
        .asyncHandler(KotlinFlowAsyncHandler::class.java)
        .beanClasses(MyDependency::class.java, MyService::class.java)
        .beanRegistrars(InvokerHelperRegistrar(MyService::class.java) { bean, factory, invokers ->
            for (name in listOf("hello", "helloThrow")) {
                val method = bean.implClazz.firstMethod(name)
                invokers[name] = factory.createInvoker(bean, method)
                    .withInstanceLookup()
                    .withArgumentLookup(0)
                    .build()
            }
        })
        .build()

    @Test
    fun test() {
        MyDependency.reset()

        val helper = Arc.container().instance(InvokerHelper::class.java).get()
        val invoker = helper.getInvoker<MyService, Flow<String>>("hello")

        assertEquals(0, MyDependency.destroyedCounter.get())

        val result: Flow<String> = invoker.invoke(null, arrayOf(null))

        assertEquals(0, MyDependency.destroyedCounter.get())

        val items = runBlocking {
            result.toList()
        }

        assertEquals(1, MyDependency.destroyedCounter.get())
        assertEquals(listOf("hello", "world"), items)
    }

    @Test
    fun testSyncThrow() {
        MyDependency.reset()

        val helper = Arc.container().instance(InvokerHelper::class.java).get()
        val invoker = helper.getInvoker<MyService, Flow<String>>("helloThrow")

        assertEquals(0, MyDependency.destroyedCounter.get())

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
        open fun hello(dep: MyDependency) = flow {
            emit("hello")
            emit("world")
        }

        open fun helloThrow(dep: MyDependency): Flow<String> {
            throw IllegalArgumentException("synchronous throw")
        }
    }
}
