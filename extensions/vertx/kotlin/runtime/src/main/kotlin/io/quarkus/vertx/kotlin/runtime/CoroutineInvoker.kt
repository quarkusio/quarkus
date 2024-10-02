package io.quarkus.vertx.kotlin.runtime

import io.quarkus.arc.Arc
import io.vertx.core.Vertx
import jakarta.enterprise.invoke.Invoker
import java.util.concurrent.CompletionStage
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture

object CoroutineInvoker {
    @JvmStatic
    fun <T> inNewCoroutine(
        instance: T,
        arguments: Array<Any?>,
        invoker: Invoker<T, Unit>
    ): CompletionStage<Void?> {
        val coroutineScope = Arc.container().instance(ApplicationCoroutineScope::class.java).get()
        val dispatcher: CoroutineDispatcher =
            Vertx.currentContext()?.let(::VertxDispatcher)
                ?: throw IllegalStateException("No Vertx context found")

        return coroutineScope
            .async<Void?>(context = dispatcher) {
                suspendCoroutine<Unit> { continuation ->
                    val newArguments = arrayOfNulls<Any>(arguments.size + 1)
                    arguments.copyInto(newArguments)
                    newArguments[arguments.size] = continuation
                    invoker.invoke(instance, newArguments)
                }
                null
            }
            .asCompletableFuture()
    }
}
