package io.quarkus.smallrye.reactivemessaging.runtime.kotlin

import io.quarkus.arc.Arc
import io.smallrye.reactive.messaging.Invoker
import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.util.concurrent.CompletableFuture

abstract class AbstractSubscribingCoroutineInvoker(private val beanInstance: Any): Invoker {

    override fun invoke(vararg args: Any?): CompletableFuture<Any?> {
        val coroutineScope = Arc.container().instance(ApplicationCoroutineScope::class.java).get()
        val dispatcher: CoroutineDispatcher = Vertx.currentContext()?.let(::VertxDispatcher)
                ?: throw IllegalStateException("No Vertx context found")

        return coroutineScope.async(context = dispatcher) {
            invokeBean(beanInstance, args)
        }.asCompletableFuture()
    }

    abstract suspend fun invokeBean(beanInstance: Any, args: Array<out Any?>): Any?
}
