package io.quarkus.smallrye.reactivemessaging.runtime.kotlin

import io.quarkus.arc.Arc
import io.smallrye.reactive.messaging.Invoker
import io.vertx.core.Vertx
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture

abstract class AbstractSubscribingCoroutineInvoker(private val beanInstance: Any) : Invoker {

    override fun invoke(vararg args: Any?): CompletableFuture<Any?> {
        val coroutineScope = Arc.container().instance(ApplicationCoroutineScope::class.java).get()
        val dispatcher =
            Vertx.currentContext()?.let(::VertxDispatcher)
                ?: throw IllegalStateException(
                    "No Vertx context found. Consider using @NonBlocking on the caller method, or make sure the upstream emits items on the Vert.x context"
                )

        return coroutineScope
            .async(context = dispatcher) {
                try {
                    invokeBean(beanInstance, args)
                } finally {
                    dispatcher.cleanup()
                }
            }
            .asCompletableFuture()
    }

    abstract suspend fun invokeBean(beanInstance: Any, args: Array<out Any?>): Any?
}
